package com.sans.finance.data.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.model.UserPreferences
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

    fun isDatabasePopulated(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() < 1024L) return false
        return runCatching {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                val cursor = db.rawQuery("SELECT count(*) FROM expenses", null)
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()
                count > 0
            }
        }.getOrDefault(false)
    }

    suspend fun uploadDatabaseBackup(
        context: Context,
        dbFile: File,
        prefs: UserPreferences
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isDatabasePopulated(dbFile)) {
            Log.w("CloudStorageSyncer", "Refusing to upload empty database (0 expenses found in ${dbFile.name}).")
            return@withContext Result.failure(Exception("Cannot upload empty database: 0 expenses found."))
        }
        val provider = getActiveProvider(prefs)
        when (provider) {
            CloudStorageProvider.CLOUDFLARE_R2 -> uploadDatabaseBackupToR2(context, dbFile, prefs)
            CloudStorageProvider.GCS -> uploadDatabaseBackupToGcs(context, dbFile, prefs)
        }
    }

    suspend fun downloadDatabaseBackup(
        context: Context,
        destFile: File,
        prefs: UserPreferences
    ): Result<File> = withContext(Dispatchers.IO) {
        val provider = getActiveProvider(prefs)
        when (provider) {
            CloudStorageProvider.CLOUDFLARE_R2 -> downloadDatabaseBackupFromR2(context, destFile, prefs)
            CloudStorageProvider.GCS -> Result.failure(Exception("GCS backup download not supported"))
        }
    }

    suspend fun downloadLatestSnapshot(
        context: Context,
        prefs: UserPreferences
    ): SnapshotImportResult = withContext(Dispatchers.IO) {
        val provider = getActiveProvider(prefs)
        when (provider) {
            CloudStorageProvider.CLOUDFLARE_R2 -> downloadLatestSnapshotFromR2(context, prefs)
            CloudStorageProvider.GCS -> downloadLatestSnapshotFromGcs(context, prefs)
        }
    }

    fun getActiveProvider(prefs: UserPreferences): CloudStorageProvider {
        val key = prefs.cloudBackupProvider
        return if (key.equals("GCS", ignoreCase = true) || key.equals("GOOGLE_CLOUD_STORAGE", ignoreCase = true)) {
            CloudStorageProvider.GCS
        } else {
            CloudStorageProvider.CLOUDFLARE_R2
        }
    }

    fun loadR2Config(context: Context, prefs: UserPreferences): CloudflareR2Config {
        if (prefs.r2AccountId.isNotBlank() && prefs.r2AccessKeyId.isNotBlank() && prefs.r2SecretAccessKey.isNotBlank()) {
            return CloudflareR2Config(prefs.r2AccountId, prefs.r2AccessKeyId, prefs.r2SecretAccessKey, prefs.r2BucketName.ifBlank { "ichsanul-dev" })
        }

        try {
            val jsonString = context.assets.open("r2_cred.json").use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            val json = JSONObject(jsonString)
            return CloudflareR2Config(
                accountId = json.optString("account_id", ""),
                accessKeyId = json.optString("access_key_id", ""),
                secretAccessKey = json.optString("secret_access_key", ""),
                bucketName = json.optString("bucket_name", prefs.r2BucketName.ifBlank { "ichsanul-dev" })
            )
        } catch (_: Exception) {}

        return CloudflareR2Config(
            accountId = prefs.r2AccountId,
            accessKeyId = prefs.r2AccessKeyId,
            secretAccessKey = prefs.r2SecretAccessKey,
            bucketName = prefs.r2BucketName.ifBlank { "ichsanul-dev" }
        )
    }

    private suspend fun <T> retryWithExponentialBackoff(
        times: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 8000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
        return block()
    }

    private suspend fun uploadFileToR2(
        r2Config: CloudflareR2Config,
        objectKey: String,
        fileBytes: ByteArray,
        contentType: String = "application/x-sqlite3"
    ): Result<Unit> {
        val payloadHash = sha256Hex(fileBytes)
        val host = "${r2Config.accountId}.r2.cloudflarestorage.com"
        val canonicalUri = "/${r2Config.bucketName}/$objectKey"
        val endpointUrl = "https://$host$canonicalUri"

        val (amzDate, dateStamp) = getIsoTimestamps()
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
            amzDate = amzDate
        )

        return retryWithExponentialBackoff {
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
            }
            conn.outputStream.use { it.write(fileBytes) }
            if (conn.responseCode in 200..299) Result.success(Unit)
            else throw Exception("R2 upload to $objectKey failed: HTTP ${conn.responseCode}")
        }
    }

    private suspend fun uploadDatabaseBackupToR2(
        context: Context,
        dbFile: File,
        prefs: UserPreferences
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!dbFile.exists()) return@withContext Result.failure(Exception("Database file not found"))

            val r2Config = loadR2Config(context, prefs)
            if (!r2Config.isValid) return@withContext Result.failure(Exception("R2 not configured"))

            val fileBytes = dbFile.readBytes()

            // 1. Upload to latest
            val latestKey = "db/sans_finance_latest.sqlite"
            val latestResult = uploadFileToR2(r2Config, latestKey, fileBytes)
            if (latestResult.isFailure) {
                return@withContext Result.failure(latestResult.exceptionOrNull() ?: Exception("R2 latest upload failed"))
            }

            // 2. Upload timestamped archive for versioned safety (never get wiped again)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
            val archiveKey = "db/archive/sans_finance_$timestamp.sqlite"
            uploadFileToR2(r2Config, archiveKey, fileBytes)

            Result.success("R2 Backup Successful ($latestKey & $archiveKey)")
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun downloadDatabaseBackupFromR2(
        context: Context,
        destFile: File,
        prefs: UserPreferences
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val r2Config = loadR2Config(context, prefs)
            if (!r2Config.isValid) return@withContext Result.failure(Exception("R2 not configured"))

            val objectKey = "db/sans_finance_latest.sqlite"
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
                amzDate = amzDate
            )

            val tempFile = File(destFile.parentFile, "${destFile.name}.download")
            retryWithExponentialBackoff {
                val url = URL(endpointUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    setRequestProperty("Authorization", authorization)
                    setRequestProperty("Host", host)
                    setRequestProperty("x-amz-date", amzDate)
                    setRequestProperty("x-amz-content-sha256", emptyPayloadHash)
                }
                if (conn.responseCode != 200) {
                    throw Exception("R2 Download Failed: HTTP ${conn.responseCode}")
                }
                conn.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (!isDatabasePopulated(tempFile)) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Downloaded backup is empty or invalid (0 expenses)"))
            }

            if (tempFile.renameTo(destFile) || (destFile.delete() && tempFile.renameTo(destFile))) {
                Result.success(destFile)
            } else {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
                Result.success(destFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadLatestSnapshotFromR2(
        context: Context,
        prefs: UserPreferences
    ): SnapshotImportResult = withContext(Dispatchers.IO) {
        val r2Config = loadR2Config(context, prefs)
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
            amzDate = amzDate
        )

        retryWithExponentialBackoff {
            val url = URL(endpointUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", authorization)
                setRequestProperty("Host", host)
                setRequestProperty("x-amz-date", amzDate)
                setRequestProperty("x-amz-content-sha256", emptyPayloadHash)
            }
            if (conn.responseCode != 200) throw Exception("R2 Download Failed")
            PortfolioJsonImporter.parseContent(conn.inputStream.bufferedReader().use { it.readText() })
        }
    }

    private fun getIsoTimestamps(): Pair<String, String> {
        val now = Date()
        val amzFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val dateStampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
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
        val canonicalRequest = listOf(httpMethod, canonicalUri, "", canonicalHeaders, signedHeaders, payloadHash).joinToString("\n")
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

    private fun loadGcsCredentials(context: Context): JSONObject {
        val jsonString = context.assets.open("SA_cred_general.json").use { it.bufferedReader().use { r -> r.readText() } }
        return JSONObject(jsonString)
    }

    private fun generateGcsJwt(clientEmail: String, privateKeyPem: String): String {
        val iat = System.currentTimeMillis() / 1000
        val exp = iat + 3600
        val header = JSONObject().apply { put("alg", "RS256"); put("typ", "JWT") }
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
        val signature = Signature.getInstance("SHA256withRSA").apply { initSign(privateKey); update(stringToSign.toByteArray(Charsets.UTF_8)) }
        return "$stringToSign.${base64UrlEncode(signature.sign())}"
    }

    private fun base64UrlEncode(input: ByteArray): String = Base64.encodeToString(input, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE).trim()

    private fun parseRsaPrivateKey(pem: String): java.security.PrivateKey {
        val der = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace("\\s".toRegex(), "")
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.decode(der, Base64.DEFAULT)))
    }

    private suspend fun getGcsAccessToken(context: Context): String = withContext(Dispatchers.IO) {
        retryWithExponentialBackoff {
            val creds = loadGcsCredentials(context)
            val assertion = generateGcsJwt(creds.getString("client_email"), creds.getString("private_key"))
            val params = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") + "&assertion=" + URLEncoder.encode(assertion, "UTF-8")
            val conn = (URL(GCS_OAUTH_TOKEN_URL).openConnection() as HttpURLConnection).apply { requestMethod = "POST"; doOutput = true; setRequestProperty("Content-Type", "application/x-www-form-urlencoded") }
            conn.outputStream.use { it.write(params.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode != 200) throw Exception("GCS Token Failed")
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getString("access_token")
        }
    }

    private suspend fun downloadLatestSnapshotFromGcs(
        context: Context,
        prefs: UserPreferences
    ): SnapshotImportResult = withContext(Dispatchers.IO) {
        val bucketName = prefs.gcsBucketName.ifBlank { "ichsanul-portfolio-snapshots" }
        val token = getGcsAccessToken(context)
        retryWithExponentialBackoff {
            val conn = (URL("https://storage.googleapis.com/storage/v1/b/$bucketName/o/snapshots%2Flatest.json?alt=media").openConnection() as HttpURLConnection).apply { setRequestProperty("Authorization", "Bearer $token") }
            if (conn.responseCode != 200) throw Exception("GCS Download Failed")
            PortfolioJsonImporter.parseContent(conn.inputStream.bufferedReader().use { it.readText() })
        }
    }

    private suspend fun uploadDatabaseBackupToGcs(
        context: Context,
        dbFile: File,
        prefs: UserPreferences
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bucketName = prefs.gcsBucketName.ifBlank { "ichsanul-portfolio-snapshots" }
            val token = getGcsAccessToken(context)
            val url = URL("https://storage.googleapis.com/upload/storage/v1/b/$bucketName/o?uploadType=media&name=" + URLEncoder.encode("db/sans_finance_latest.sqlite", "UTF-8"))
            retryWithExponentialBackoff {
                val conn = (url.openConnection() as HttpURLConnection).apply { requestMethod = "POST"; doOutput = true; setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/x-sqlite3"); setFixedLengthStreamingMode(dbFile.length()) }
                dbFile.inputStream().use { it.copyTo(conn.outputStream) }
                if (conn.responseCode in 200..299) Result.success("GCS Backup Successful")
                else throw Exception("GCS Upload Failed: ${conn.responseCode}")
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}
