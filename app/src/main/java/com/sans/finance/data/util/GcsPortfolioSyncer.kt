package com.sans.finance.data.util

import android.content.Context
import android.util.Base64
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

object GcsPortfolioSyncer {

    private const val BUCKET_NAME = "ichsanul-portfolio-snapshots"
    private const val OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"

    // Load credentials from assets
    private fun loadCredentials(context: Context): JSONObject {
        val jsonString = context.assets.open("SA_cred_general.json").use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        return JSONObject(jsonString)
    }

    // Generate JWT Assertion for Google OAuth2
    private fun generateJwt(clientEmail: String, privateKeyPem: String): String {
        val iat = System.currentTimeMillis() / 1000
        val exp = iat + 3600

        val header = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }

        val claims = JSONObject().apply {
            put("iss", clientEmail)
            put("scope", "https://www.googleapis.com/auth/devstorage.read_only")
            put("aud", OAUTH_TOKEN_URL)
            put("exp", exp)
            put("iat", iat)
        }

        val headerBase64 = base64UrlEncode(header.toString().toByteArray(Charsets.UTF_8))
        val claimsBase64 = base64UrlEncode(claims.toString().toByteArray(Charsets.UTF_8))
        val stringToSign = "$headerBase64.$claimsBase64"

        val privateKey = parsePrivateKey(privateKeyPem)
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

    private fun parsePrivateKey(pem: String): java.security.PrivateKey {
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

    // Request Access Token from Google OAuth2
    private suspend fun getAccessToken(context: Context): String = withContext(Dispatchers.IO) {
        val creds = loadCredentials(context)
        val clientEmail = creds.getString("client_email")
        val privateKey = creds.getString("private_key")

        val assertion = generateJwt(clientEmail, privateKey)
        val params = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
                "&assertion=" + URLEncoder.encode(assertion, "UTF-8")

        val url = URL(OAUTH_TOKEN_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        conn.outputStream.use { os ->
            os.write(params.toByteArray(Charsets.UTF_8))
        }

        if (conn.responseCode != 200) {
            val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Failed to get OAuth token: ${conn.responseCode} - $errStream")
        }

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        json.getString("access_token")
    }

    // Direct 1-shot fetch of latest snapshot from GCS
    suspend fun downloadLatestSnapshot(context: Context): Triple<Long, List<PortfolioHoldingEntity>, Double?> = withContext(Dispatchers.IO) {
        val token = getAccessToken(context)

        val latestUrl = URL("https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o/snapshots%2Flatest.json?alt=media")
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
}
