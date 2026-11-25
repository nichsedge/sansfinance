package com.sans.finance.data.util

import android.content.Context
import android.util.Log
import com.sans.finance.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

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
        uploadDatabaseBackupToGateway(dbFile, prefs)
    }

    suspend fun downloadDatabaseBackup(
        context: Context,
        destFile: File,
        prefs: UserPreferences
    ): Result<File> = withContext(Dispatchers.IO) {
        downloadDatabaseBackupFromGateway(destFile, prefs)
    }

    suspend fun downloadLatestSnapshot(
        context: Context,
        prefs: UserPreferences
    ): SnapshotImportResult = withContext(Dispatchers.IO) {
        downloadLatestSnapshotFromGateway(prefs)
    }

    private suspend fun downloadLatestSnapshotFromGateway(
        prefs: UserPreferences
    ): SnapshotImportResult = withContext(Dispatchers.IO) {
        val baseUrl = prefs.syncGatewayUrl.trimEnd('/')
        val token = prefs.syncGatewayToken.trim()
        if (token.isBlank()) {
            throw Exception("Sync Auth Token is not configured. Please set your token in Settings.")
        }
        val url = URL("$baseUrl/api/portfolio/snapshot")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Sync-Token", token)
            connectTimeout = 15000
            readTimeout = 45000
        }
        val code = conn.responseCode
        if (code == 200) {
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            PortfolioJsonImporter.parseContent(jsonText)
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw Exception("Gateway portfolio download failed ($code): $err")
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
            val tempFile = File(destFile.parentFile, "${destFile.name}.download")
            conn.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
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
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            Result.failure(Exception("Gateway download failed ($code): $err"))
        }
    }
}
