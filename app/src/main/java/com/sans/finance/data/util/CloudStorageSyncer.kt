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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class CloudStorageProvider(val label: String) {
    GATEWAY("Cloud Sync Gateway (Cloud Run & R2)"),
    CLOUDFLARE_R2("Cloudflare R2 (Legacy Direct S3)")
}

data class InboxExpense(
    val id: String,
    val source: String,
    val messageId: String,
    val merchant: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val paymentMethod: String,
    val receivedAt: String,
    val type: String = "EXPENSE"
)

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
            CloudStorageProvider.GATEWAY -> uploadDatabaseBackupToGateway(dbFile, prefs)
            CloudStorageProvider.CLOUDFLARE_R2 -> uploadDatabaseBackupToR2(context, dbFile, prefs)
        }
    }

    suspend fun downloadDatabaseBackup(
        context: Context,
        destFile: File,
        prefs: UserPreferences
    ): Result<File> = withContext(Dispatchers.IO) {
        val provider = getActiveProvider(prefs)
        when (provider) {
            CloudStorageProvider.GATEWAY -> downloadDatabaseBackupFromGateway(destFile, prefs)
            CloudStorageProvider.CLOUDFLARE_R2 -> downloadDatabaseBackupFromR2(destFile, prefs)
        }
    }

    suspend fun downloadLatestSnapshot(
        context: Context,
        prefs: UserPreferences
    ): SnapshotImportResult = withContext(Dispatchers.IO) {
        downloadLatestSnapshotFromR2(prefs)
    }

    fun getActiveProvider(prefs: UserPreferences): CloudStorageProvider {
        val key = prefs.cloudBackupProvider
        return when {
            key.equals("CLOUDFLARE_R2", ignoreCase = true) || key.equals("R2", ignoreCase = true) -> CloudStorageProvider.CLOUDFLARE_R2
            else -> CloudStorageProvider.GATEWAY
        }
    }

    suspend fun fetchInboxExpenses(prefs: UserPreferences): Result<List<InboxExpense>> = withContext(Dispatchers.IO) {
        val baseUrl = prefs.syncGatewayUrl.trimEnd('/')
        val token = prefs.syncGatewayToken.trim()
        if (token.isBlank()) {
            return@withContext Result.failure(Exception("Sync Auth Token is not configured."))
        }
        try {
            val url = URL("$baseUrl/api/inbox")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-Sync-Token", token)
                connectTimeout = 15000
                readTimeout = 20000
            }
            if (conn.responseCode != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP ${conn.responseCode}"
                return@withContext Result.failure(Exception("Failed to fetch inbox: $err"))
            }
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val itemsJson = root.optJSONArray("items") ?: return@withContext Result.success(emptyList())
            val list = mutableListOf<InboxExpense>()
            for (i in 0 until itemsJson.length()) {
                val obj = itemsJson.getJSONObject(i)
                list.add(
                    InboxExpense(
                        id = obj.optString("id"),
                        source = obj.optString("source", "gmail"),
                        messageId = obj.optString("message_id"),
                        merchant = obj.optString("merchant", "Unknown"),
                        amount = obj.optDouble("amount", 0.0),
                        currency = obj.optString("currency", "IDR"),
                        category = obj.optString("category", "Misc"),
                        paymentMethod = obj.optString("payment_method", "Unknown"),
                        receivedAt = obj.optString("received_at"),
                        type = obj.optString("type", "EXPENSE")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acknowledgeInboxExpenses(ids: List<String>, prefs: UserPreferences): Result<Int> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(0)
        val baseUrl = prefs.syncGatewayUrl.trimEnd('/')
        val token = prefs.syncGatewayToken.trim()
        if (token.isBlank()) return@withContext Result.success(0)
        try {
            val url = URL("$baseUrl/api/inbox/ack")
            val payload = JSONObject().apply {
                put("ids", org.json.JSONArray(ids))
            }.toString()

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-Sync-Token", token)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode == 200) {
                val resp = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                Result.success(resp.optInt("acknowledged_count", ids.size))
            } else {
                Result.failure(Exception("Ack failed HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadDatabaseBackupToGateway(
        dbFile: File,
        prefs: UserPreferences
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = prefs.syncGatewayUrl.trimEnd('/')
        val token = prefs.syncGatewayToken.trim()
        if (token.isBlank()) {
            return@withContext Result.failure(Exception("Sync Auth Token is not configured. Please set your token in Settings."))
        }
        val url = URL("$baseUrl/api/backup")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Sync-Token", token)
            setRequestProperty("Content-Type", "application/x-sqlite3")
            setFixedLengthStreamingMode(dbFile.length())
            connectTimeout = 15000
            readTimeout = 45000
        }
        dbFile.inputStream().use { input ->
            conn.outputStream.use { output ->
                input.copyTo(output)
            }
        }
        val code = conn.responseCode
        if (code in 200..299) {
            Result.success("Cloud Backup Successful via Gateway")
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            Result.failure(Exception("Gateway upload failed ($code): $err"))
        }
    }

    private suspend fun downloadDatabaseBackupFromGateway(
        destFile: File,
        prefs: UserPreferences
    ): Result<File> = withContext(Dispatchers.IO) {
        val baseUrl = prefs.syncGatewayUrl.trimEnd('/')
        val token = prefs.syncGatewayToken.trim()
        if (token.isBlank()) {
            return@withContext Result.failure(Exception("Sync Auth Token is not configured. Please set your token in Settings."))
        }
        val url = URL("$baseUrl/api/backup")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Sync-Token", token)
            connectTimeout = 15000
            readTimeout = 45000
        }
        val code = conn.responseCode
        if (code == 200) {
            conn.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(destFile)
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            Result.failure(Exception("Gateway download failed ($code): $err"))
        }
    }

    fun loadR2Config(prefs: UserPreferences): CloudflareR2Config {
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

            val r2Config = loadR2Config(prefs)
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
        destFile: File,
        prefs: UserPreferences
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val r2Config = loadR2Config(prefs)
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
        prefs: UserPreferences
    ): SnapshotImportResult = withContext(Dispatchers.IO) {
        val r2Config = loadR2Config(prefs)
        val objectKey = "portfolio/latest_snapshot.json"
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
}
