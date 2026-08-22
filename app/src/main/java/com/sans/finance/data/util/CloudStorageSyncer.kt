package com.sans.finance.data.util

import android.content.Context
import android.util.Base64
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class CloudStorageProvider(val label: String) {
    GCS("Google Cloud Storage (GCS)"),
    CLOUDFLARE_R2("Cloudflare R2 (S3-Compatible)")
}

data class CloudflareR2Config(
    val accountId: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String = "ichsanul-dev"
) {
    val isValid: Boolean
        get() = accountId.isNotBlank() && accessKeyId.isNotBlank() && secretAccessKey.isNotBlank() && bucketName.isNotBlank()
}

object CloudStorageSyncer {

    private const val GCS_OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"

    // =========================================================================
    // Public Unified API
    // =========================================================================

    suspend fun uploadDatabaseBackup(
        context: Context,
        dbFile: File,
        localeManager: LocaleManager? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val provider = getActiveProvider(localeManager)
        when (provider) {
            CloudStorageProvider.CLOUDFLARE_R2 -> uploadDatabaseBackupToR2(context, dbFile, localeManager)
            CloudStorageProvider.GCS -> uploadDatabaseBackupToGcs(context, dbFile, localeManager)
        }
    }

    suspend fun downloadLatestSnapshot(
        context: Context,
        localeManager: LocaleManager? = null
    ): Triple<Long, List<PortfolioHoldingEntity>, Double?> = withContext(Dispatchers.IO) {
        val provider = getActiveProvider(localeManager)
        when (provider) {
            CloudStorageProvider.CLOUDFLARE_R2 -> downloadLatestSnapshotFromR2(context, localeManager)
            CloudStorageProvider.GCS -> downloadLatestSnapshotFromGcs(context, localeManager)
        }
    }

    fun getActiveProvider(localeManager: LocaleManager?): CloudStorageProvider {
        val key = localeManager?.getCloudBackupProvider() ?: "GCS"
        return if (key.equals("CLOUDFLARE_R2", ignoreCase = true) || key.equals("R2", ignoreCase = true)) {
            CloudStorageProvider.CLOUDFLARE_R2
        } else {
            CloudStorageProvider.GCS
        }
    }

    // =========================================================================
    // Cloudflare R2 Implementation (AWS SigV4 over HTTPS)
    // =========================================================================

    fun loadR2Config(context: Context, localeManager: LocaleManager?): CloudflareR2Config {
        // 1. Check user preferences from LocaleManager
        if (localeManager != null) {
            val accId = localeManager.getR2AccountId()
            val keyId = localeManager.getR2AccessKeyId()
            val secKey = localeManager.getR2SecretAccessKey()
            val bucket = localeManager.getR2BucketName().ifBlank { "ichsanul-dev" }
            if (accId.isNotBlank() && keyId.isNotBlank() && secKey.isNotBlank()) {
                return CloudflareR2Config(accId, keyId, secKey, bucket)
            }
        }

        // 2. Fallback to assets/r2_cred.json if present
        try {
            val jsonString = context.assets.open("r2_cred.json").use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            val json = JSONObject(jsonString)
            return CloudflareR2Config(
                accountId = json.optString("account_id", ""),
                accessKeyId = json.optString("access_key_id", ""),
                secretAccessKey = json.optString("secret_access_key", ""),
                bucketName = json.optString("bucket_name", localeManager?.getR2BucketName() ?: "ichsanul-dev")
            )
        } catch (_: Exception) {
            // Asset not present
        }

        return CloudflareR2Config(
            accountId = localeManager?.getR2AccountId() ?: "",
            accessKeyId = localeManager?.getR2AccessKeyId() ?: "",
            secretAccessKey = localeManager?.getR2SecretAccessKey() ?: "",
            bucketName = localeManager?.getR2BucketName() ?: "ichsanul-dev"
        )
    }

    private suspend fun uploadDatabaseBackupToR2(
        context: Context,
        dbFile: File,
        localeManager: LocaleManager?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found: ${dbFile.absolutePath}"))
            }

            val r2Config = loadR2Config(context, localeManager)
            if (!r2Config.isValid) {
                return@withContext Result.failure(
                    Exception("Cloudflare R2 is not configured. Please configure Account ID, Access Key ID, and Secret Access Key.")
                )
            }

            val objectKey = "db/sans_finance_latest.sqlite"
            val fileBytes = dbFile.readBytes()
            val payloadHash = sha256Hex(fileBytes)

            val host = "${r2Config.accountId}.r2.cloudflarestorage.com"
            val canonicalUri = "/${r2Config.bucketName}/$objectKey"
            val endpointUrl = "https://$host$canonicalUri"

            val (amzDate, dateStamp) = getIsoTimestamps()
            val contentType = "application/x-sqlite3"

            val headers = sortedMapOf(
                "content-type" to contentType,
                "host" to host,
                "x-amz-content-sha256" to payloadHash,
                "x-amz-date" to amzDate
            )

            val authorization = buildSigV4AuthorizationHeader(
                httpMethod = "PUT",
                canonicalUri = canonicalUri,
                headers = headers,
                payloadHash = payloadHash,
                accessKey = r2Config.accessKeyId,
                secretKey = r2Config.secretAccessKey,
                dateStamp = dateStamp,
                amzDate = amzDate,
                region = "auto",
                service = "s3"
            )

            val url = URL(endpointUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Authorization", authorization)
                setRequestProperty("Content-Type", contentType)
                setRequestProperty("Host", host)
                setRequestProperty("x-amz-date", amzDate)
                setRequestProperty("x-amz-content-sha256", payloadHash)
                setFixedLengthStreamingMode(fileBytes.size)
                connectTimeout = 15000
                readTimeout = 30000
            }

            conn.outputStream.use { os ->
                os.write(fileBytes)
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Result.success("Cloudflare R2 Backup Successful (r2://${r2Config.bucketName}/$objectKey)")
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Result.failure(Exception("Cloudflare R2 upload failed ($responseCode): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadLatestSnapshotFromR2(
        context: Context,
        localeManager: LocaleManager?
    ): Triple<Long, List<PortfolioHoldingEntity>, Double?> = withContext(Dispatchers.IO) {
        val r2Config = loadR2Config(context, localeManager)
        if (!r2Config.isValid) {
            throw Exception("Cloudflare R2 is not configured. Please fill credentials in Settings.")
        }

        val objectKey = "snapshots/latest.json"
        val host = "${r2Config.accountId}.r2.cloudflarestorage.com"
        val canonicalUri = "/${r2Config.bucketName}/$objectKey"
        val endpointUrl = "https://$host$canonicalUri"

        val (amzDate, dateStamp) = getIsoTimestamps()
        val emptyPayloadHash = sha256Hex(ByteArray(0))

        val headers = sortedMapOf(
            "host" to host,
            "x-amz-content-sha256" to emptyPayloadHash,
            "x-amz-date" to amzDate
        )

        val authorization = buildSigV4AuthorizationHeader(
            httpMethod = "GET",
            canonicalUri = canonicalUri,
            headers = headers,
            payloadHash = emptyPayloadHash,
            accessKey = r2Config.accessKeyId,
            secretKey = r2Config.secretAccessKey,
            dateStamp = dateStamp,
            amzDate = amzDate,
            region = "auto",
            service = "s3"
        )

        val url = URL(endpointUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", authorization)
            setRequestProperty("Host", host)
            setRequestProperty("x-amz-date", amzDate)
            setRequestProperty("x-amz-content-sha256", emptyPayloadHash)
            connectTimeout = 8000
            readTimeout = 15000
        }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Failed to download snapshots/latest.json from Cloudflare R2: $responseCode - $errStream")
        }

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        PortfolioJsonImporter.parseContent(jsonString)
    }

    // =========================================================================
    // AWS SigV4 Pure-Kotlin Implementation
    // =========================================================================

    private fun getIsoTimestamps(): Pair<String, String> {
        val now = Date()
        val amzFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateStampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return Pair(amzFormat.format(now), dateStampFormat.format(now))
    }

    private fun buildSigV4AuthorizationHeader(
        httpMethod: String,
        canonicalUri: String,
        headers: Map<String, String>,
        payloadHash: String,
        accessKey: String,
        secretKey: String,
        dateStamp: String,
        amzDate: String,
        region: String = "auto",
        service: String = "s3"
    ): String {
        val canonicalHeaders = headers.entries.joinToString("") { (k, v) -> "${k.lowercase(Locale.US)}:$v\n" }
        val signedHeaders = headers.keys.joinToString(";") { it.lowercase(Locale.US) }

        val canonicalRequest = listOf(
            httpMethod,
            canonicalUri,
            "", // Query parameters
            canonicalHeaders,
            signedHeaders,
            payloadHash
        ).joinToString("\n")

        val canonicalRequestHash = sha256Hex(canonicalRequest.toByteArray(StandardCharsets.UTF_8))
        val credentialScope = "$dateStamp/$region/$service/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n$canonicalRequestHash"

        val signingKey = getSignatureKey(secretKey, dateStamp, region, service)
        val signature = bytesToHex(hmacSha256(signingKey, stringToSign))

        return "AWS4-HMAC-SHA256 Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4" + key).toByteArray(StandardCharsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return bytesToHex(md.digest(data))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexDigits = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexDigits[v ushr 4]
            hexChars[i * 2 + 1] = hexDigits[v and 0x0F]
        }
        return String(hexChars)
    }

    // =========================================================================
    // Google Cloud Storage Implementation (OAuth2 / Service Account JWT)
    // =========================================================================

    private fun loadGcsCredentials(context: Context): JSONObject {
        val jsonString = context.assets.open("SA_cred_general.json").use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        return JSONObject(jsonString)
    }

    private fun generateGcsJwt(clientEmail: String, privateKeyPem: String): String {
        val iat = System.currentTimeMillis() / 1000
        val exp = iat + 3600

        val header = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }

        val claims = JSONObject().apply {
            put("iss", clientEmail)
            put("scope", "https://www.googleapis.com/auth/devstorage.read_write")
            put("aud", GCS_OAUTH_TOKEN_URL)
            put("exp", exp)
            put("iat", iat)
        }

        val headerBase64 = base64UrlEncode(header.toString().toByteArray(Charsets.UTF_8))
        val claimsBase64 = base64UrlEncode(claims.toString().toByteArray(Charsets.UTF_8))
        val stringToSign = "$headerBase64.$claimsBase64"

        val privateKey = parseRsaPrivateKey(privateKeyPem)
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(stringToSign.toByteArray(Charsets.UTF_8))
        }
        val signatureBytes = signature.sign()
        val signatureBase64 = base64UrlEncode(signatureBytes)

        return "$stringToSign.$signatureBase64"
    }

    private fun base64UrlEncode(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE).trim()
    }

    private fun parseRsaPrivateKey(pem: String): java.security.PrivateKey {
        val privateKeyDer = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .replace("\n", "")

        val keyBytes = Base64.decode(privateKeyDer, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    private suspend fun getGcsAccessToken(context: Context): String = withContext(Dispatchers.IO) {
        val creds = loadGcsCredentials(context)
        val clientEmail = creds.getString("client_email")
        val privateKey = creds.getString("private_key")

        val assertion = generateGcsJwt(clientEmail, privateKey)
        val params = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
                "&assertion=" + URLEncoder.encode(assertion, "UTF-8")

        val url = URL(GCS_OAUTH_TOKEN_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        conn.outputStream.use { os ->
            os.write(params.toByteArray(Charsets.UTF_8))
        }

        if (conn.responseCode != 200) {
            val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Failed to get OAuth token from Google: ${conn.responseCode} - $errStream")
        }

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        json.getString("access_token")
    }

    private suspend fun downloadLatestSnapshotFromGcs(
        context: Context,
        localeManager: LocaleManager?
    ): Triple<Long, List<PortfolioHoldingEntity>, Double?> = withContext(Dispatchers.IO) {
        val bucketName = localeManager?.getGcsBucketName()?.ifBlank { "ichsanul-portfolio-snapshots" } ?: "ichsanul-portfolio-snapshots"
        val token = getGcsAccessToken(context)

        val latestUrl = URL("https://storage.googleapis.com/storage/v1/b/$bucketName/o/snapshots%2Flatest.json?alt=media")
        val conn = latestUrl.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = 5000
        conn.readTimeout = 10000

        if (conn.responseCode != 200) {
            val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Failed to download snapshots/latest.json from GCS: ${conn.responseCode} - $errStream")
        }

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        PortfolioJsonImporter.parseContent(jsonString)
    }

    private suspend fun uploadDatabaseBackupToGcs(
        context: Context,
        dbFile: File,
        localeManager: LocaleManager?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found: ${dbFile.absolutePath}"))
            }

            val bucketName = localeManager?.getGcsBucketName()?.ifBlank { "ichsanul-portfolio-snapshots" } ?: "ichsanul-portfolio-snapshots"
            val token = getGcsAccessToken(context)
            val objectName = "db/sans_finance_latest.sqlite"
            val encodedName = URLEncoder.encode(objectName, "UTF-8")
            val uploadUrl = URL("https://storage.googleapis.com/upload/storage/v1/b/$bucketName/o?uploadType=media&name=$encodedName")

            val conn = uploadUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/x-sqlite3")
            conn.setFixedLengthStreamingMode(dbFile.length())
            conn.connectTimeout = 10000
            conn.readTimeout = 30000

            dbFile.inputStream().use { input ->
                conn.outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            if (conn.responseCode in 200..299) {
                Result.success("Google Cloud Storage Backup Successful (gs://$bucketName/$objectName)")
            } else {
                val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Result.failure(Exception("GCS Upload failed (${conn.responseCode}): $errStream"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
