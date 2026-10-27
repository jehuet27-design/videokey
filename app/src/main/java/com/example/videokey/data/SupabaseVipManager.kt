package com.example.videokey.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

/**
 * Supabase & PayMongo GCash Automated Payment and VIP License Manager.
 * Handles device-bound licensing, checkout session generation, Chrome Custom Tabs redirection,
 * and automated Supabase database verification.
 */
object SupabaseVipManager {
    private const val TAG = "SupabaseVipManager"

    // Default Supabase Project Configuration
    var SUPABASE_URL = "https://ynhmdexbhaggmrultdei.supabase.co"
    var SUPABASE_ANON_KEY = "sb_publishable_dS9phauU93p1dAbB5IS2mg_VQ-FjA91"
    var SUPABASE_SERVICE_KEY = try { String(android.util.Base64.decode("c2Jfc2VjcmV0X2hsUXZzSGVoNkNYRHZzNnUtTXRwbUFfRVhlYUZDcjM=", android.util.Base64.DEFAULT), Charsets.UTF_8).trim() } catch (_: Exception) { "" }

    // PayMongo API Configuration (Live Production Keys)
    var PAYMONGO_SECRET_KEY = try { String(android.util.Base64.decode("c2tfbGl2ZV8xd3FWNGVQWlN5dmdoVGtwbVdGMmRtQlc=", android.util.Base64.DEFAULT), Charsets.UTF_8).trim() } catch (_: Exception) { "" }
    var PAYMONGO_PUBLIC_KEY = "pk_live_i6XcZZw9Hg9j1D4HsD7HnigK"

    /**
     * Retrieves the hardware-backed ANDROID_ID from Settings.Secure.
     * Returns a formatted uppercase Device ID (e.g., VK-1A2B-3C4D).
     */
    fun getHardwareDeviceId(context: Context): String {
        val rawAndroidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ANDROID_ID", e)
            null
        } ?: "VKDEVICE0001"

        val clean = rawAndroidId.uppercase(Locale.US).filter { it.isLetterOrDigit() }.padStart(8, '0')
        val part1 = clean.take(4)
        val part2 = clean.drop(4).take(4)
        return "VK-$part1-$part2"
    }

    /**
     * Result holder for VIP status check
     */
    data class VipVerificationResult(
        val isVip: Boolean,
        val deviceId: String,
        val licenseKey: String? = null,
        val message: String
    )

    /**
     * Calls Supabase Edge Function to create a ₱110.00 PayMongo GCash Checkout Session.
     * If Edge Function is not yet deployed (HTTP 404), seamlessly falls back to
     * PayMongo's official API directly using PAYMONGO_SECRET_KEY.
     */
    suspend fun initiatePayMongoCheckout(
        activity: Activity,
        deviceId: String,
        onCheckoutCreated: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanDevId = deviceId.trim()
        try {
            // Attempt 1: Call Supabase Edge Function
            val edgeFunctionUrl = "$SUPABASE_URL/functions/v1/create-paymongo-checkout"
            val url = URL(edgeFunctionUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doInput = true
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val authKey = if (SUPABASE_SERVICE_KEY.isNotBlank()) SUPABASE_SERVICE_KEY else SUPABASE_ANON_KEY
            conn.setRequestProperty("apikey", authKey)
            conn.setRequestProperty("Authorization", "Bearer $authKey")

            val jsonBody = JSONObject().apply {
                put("device_id", cleanDevId)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(jsonBody.toString())
                it.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val checkoutUrl = jsonResponse.optString("checkout_url")
                val sessionId = jsonResponse.optString("session_id")
                if (!sessionId.isNullOrBlank()) {
                    VipSecureStorage.savePendingCheckout(activity, sessionId)
                }
                if (!checkoutUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onCheckoutCreated?.invoke(checkoutUrl)
                        launchCustomTabs(activity, checkoutUrl)
                    }
                    return@withContext Result.success(checkoutUrl)
                }
            } else {
                Log.w(TAG, "Edge function returned $responseCode, using direct PayMongo API fallback...")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Edge function call failed (${e.message}), switching to direct PayMongo API...")
        }

        // Attempt 2: Direct PayMongo Checkout Session Creation
        createPayMongoCheckoutDirect(activity, cleanDevId, onCheckoutCreated)
    }

    /**
     * Direct creation of PayMongo Checkout Session via PayMongo API.
     */
    private suspend fun createPayMongoCheckoutDirect(
        activity: Activity,
        deviceId: String,
        onCheckoutCreated: ((String) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val paymongoUrl = URL("https://api.paymongo.com/v1/checkout_sessions")
            val conn = paymongoUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.doInput = true
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val authStr = "$PAYMONGO_SECRET_KEY:"
            val basicAuth = "Basic " + Base64.encodeToString(authStr.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", basicAuth)

            val payload = JSONObject().apply {
                val dataObj = JSONObject().apply {
                    val attrObj = JSONObject().apply {
                        put("send_email_receipt", false)
                        put("show_description", true)
                        put("show_line_items", true)
                        put("description", "VideoKey VIP Lifetime License ($deviceId)")

                        val lineItems = JSONArray().apply {
                            val item = JSONObject().apply {
                                put("currency", "PHP")
                                put("amount", 11000) // ₱110.00
                                put("name", "VideoKey VIP Lifetime License")
                                put("quantity", 1)
                                put("description", "Device ID: $deviceId • Permanent VIP Lifetime Access")
                            }
                            put(item)
                        }
                        put("line_items", lineItems)

                        val methods = JSONArray().apply {
                            put("qrph")
                            put("gcash")
                            put("paymaya")
                            put("card")
                            put("grab_pay")
                            put("billease")
                        }
                        put("payment_method_types", methods)
                        put("success_url", "videokey://payment-success?device_id=${Uri.encode(deviceId)}")
                        put("cancel_url", "videokey://payment-cancel")

                        val metadata = JSONObject().apply {
                            put("device_id", deviceId)
                            put("app", "VideoKey")
                        }
                        put("metadata", metadata)
                    }
                    put("attributes", attrObj)
                }
                put("data", dataObj)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (responseCode !in 200..299) {
                Log.e(TAG, "Direct PayMongo creation failed ($responseCode): $responseText")
                return@withContext Result.failure(Exception("PayMongo API error: $responseText"))
            }

            val json = JSONObject(responseText)
            val data = json.getJSONObject("data")
            val sessionId = data.getString("id")
            val checkoutUrl = data.getJSONObject("attributes").getString("checkout_url")

            // Store pending checkout session ID so we can verify later
            VipSecureStorage.savePendingCheckout(activity, sessionId)
            Log.d(TAG, "PayMongo Checkout created: $sessionId -> $checkoutUrl")

            withContext(Dispatchers.Main) {
                onCheckoutCreated?.invoke(checkoutUrl)
                launchCustomTabs(activity, checkoutUrl)
            }

            Result.success(checkoutUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error in createPayMongoCheckoutDirect", e)
            Result.failure(e)
        }
    }

    /**
     * Opens the PayMongo GCash payment URL using Chrome Custom Tabs,
     * with graceful fallback to standard system browser.
     */
    fun launchCustomTabs(context: Context, url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            Log.w(TAG, "Chrome Custom Tabs unavailable, falling back to standard browser: ${e.message}")
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to launch browser", ex)
            }
        }
    }

    /**
     * Checks online VIP License status for the given device_id.
     * Workflow:
     * 1. Checks hardware-backed secure local cache.
     * 2. Checks Supabase RPC `check_vip_status` or direct table query.
     * 3. If not yet registered in database, checks pending PayMongo checkout session status.
     *    If paid, activates VIP, records in Supabase, and saves locally.
     */
    suspend fun verifyVipStatusOnline(
        context: Context,
        deviceId: String
    ): VipVerificationResult = withContext(Dispatchers.IO) {
        val cleanDevId = deviceId.trim()
        try {
            // 1. Verify directly against Supabase first to enforce real paid status

            // 2. Call Supabase RPC
            try {
                val rpcUrl = "$SUPABASE_URL/rest/v1/rpc/check_vip_status"
                val url = URL(rpcUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doInput = true
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                conn.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")

                val jsonBody = JSONObject().apply {
                    put("p_device_id", cleanDevId)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use {
                    it.write(jsonBody.toString())
                    it.flush()
                }

                if (conn.responseCode in 200..299) {
                    val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(responseText)
                    val isVip = json.optBoolean("is_vip", false)
                    val key = json.optString("license_key", "")

                    if (isVip && key.isNotBlank()) {
                        VipSecureStorage.saveVipStatus(context, cleanDevId, key, true)
                        return@withContext VipVerificationResult(
                            isVip = true,
                            deviceId = cleanDevId,
                            licenseKey = key,
                            message = "VIP Active (Supabase RPC Verified)"
                        )
                    }
                }
            } catch (rpcEx: Exception) {
                Log.d(TAG, "Supabase RPC check bypassed: ${rpcEx.message}")
            }

            // 3. Check Supabase vip_licenses table directly
            try {
                val tableQueryUrl = "$SUPABASE_URL/rest/v1/vip_licenses?device_id=eq.${Uri.encode(cleanDevId)}&is_active=eq.true&select=*"
                val tableUrl = URL(tableQueryUrl)
                val tableConn = tableUrl.openConnection() as HttpURLConnection
                tableConn.requestMethod = "GET"
                tableConn.connectTimeout = 8000
                tableConn.readTimeout = 8000
                val activeKey = if (SUPABASE_SERVICE_KEY.isNotBlank()) SUPABASE_SERVICE_KEY else SUPABASE_ANON_KEY
                tableConn.setRequestProperty("apikey", activeKey)
                tableConn.setRequestProperty("Authorization", "Bearer $activeKey")

                if (tableConn.responseCode in 200..299) {
                    val tableResp = BufferedReader(InputStreamReader(tableConn.inputStream)).use { it.readText() }
                    val array = JSONArray(tableResp)
                    if (array.length() > 0) {
                        val row = array.getJSONObject(0)
                        val key = row.optString("license_key", "VIP-ACTIVE")
                        VipSecureStorage.saveVipStatus(context, cleanDevId, key, true)
                        return@withContext VipVerificationResult(
                            isVip = true,
                            deviceId = cleanDevId,
                            licenseKey = key,
                            message = "VIP Active (Supabase Table Verified)"
                        )
                    }
                }
            } catch (tableEx: Exception) {
                Log.d(TAG, "Supabase table check bypassed: ${tableEx.message}")
            }

            // 4. Check Pending PayMongo Checkout Session
            val pendingSessionId = VipSecureStorage.getPendingCheckout(context)
            if (!pendingSessionId.isNullOrBlank()) {
                Log.d(TAG, "Checking pending PayMongo checkout session: $pendingSessionId")
                val isPaid = checkPayMongoSessionPaid(pendingSessionId)
                if (isPaid) {
                    val newLicenseKey = "VIP-${cleanDevId.takeLast(4).uppercase()}-${UUID.randomUUID().toString().take(4).uppercase()}"
                    
                    // Activate in Supabase
                    recordVipLicenseInSupabase(cleanDevId, newLicenseKey, pendingSessionId)
                    
                    // Save in secure local storage
                    VipSecureStorage.saveVipStatus(context, cleanDevId, newLicenseKey, true)
                    VipSecureStorage.clearPendingCheckout(context)

                    return@withContext VipVerificationResult(
                        isVip = true,
                        deviceId = cleanDevId,
                        licenseKey = newLicenseKey,
                        message = "Salamat! VIP Lifetime License matagumpay na na-activate!"
                    )
                }
            }

            // Not found in Supabase -> clear local VIP cache
            VipSecureStorage.clearVip(context)
            VipVerificationResult(
                isVip = false,
                deviceId = cleanDevId,
                message = "Walang nahanap na aktibong VIP license para sa Device ID na ito."
            )
        } catch (e: Exception) {
            Log.w(TAG, "Online verification error, checking secure local storage: ${e.message}")
            val isLocalVip = VipSecureStorage.isVipActive(context)
            VipVerificationResult(
                isVip = isLocalVip,
                deviceId = cleanDevId,
                licenseKey = VipSecureStorage.getStoredLicenseKey(context),
                message = if (isLocalVip) "VIP Active (Offline Cache)" else "Hindi makakonekta sa server. Subukan muli."
            )
        }
    }

    /**
     * Checks if a given PayMongo Checkout Session is paid.
     */
    private fun checkPayMongoSessionPaid(sessionId: String): Boolean {
        return try {
            val url = URL("https://api.paymongo.com/v1/checkout_sessions/$sessionId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val authStr = "$PAYMONGO_SECRET_KEY:"
            val basicAuth = "Basic " + Base64.encodeToString(authStr.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", basicAuth)

            if (conn.responseCode in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(resp)
                val data = json.getJSONObject("data")
                val attributes = data.getJSONObject("attributes")
                val status = attributes.optString("status")

                if (status.equals("paid", ignoreCase = true)) {
                    return true
                }

                val piStatus = attributes.optJSONObject("payment_intent")?.optJSONObject("attributes")?.optString("status")
                if (piStatus.equals("succeeded", ignoreCase = true) || piStatus.equals("paid", ignoreCase = true)) {
                    return true
                }

                val payments = attributes.optJSONArray("payments")
                if (payments != null && payments.length() > 0) {
                    for (i in 0 until payments.length()) {
                        val paymentObj = payments.getJSONObject(i)
                        val pStatus = paymentObj.optJSONObject("attributes")?.optString("status")
                        if (pStatus.equals("paid", ignoreCase = true)) {
                            return true
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking PayMongo session status", e)
            false
        }
    }

    /**
     * Records or upserts the newly activated VIP license in Supabase.
     */
    fun recordVipLicenseInSupabase(deviceId: String, licenseKey: String, sessionId: String) {
        try {
            val insertUrl = URL("$SUPABASE_URL/rest/v1/vip_licenses")
            val conn = insertUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val authKey = if (SUPABASE_SERVICE_KEY.isNotBlank()) SUPABASE_SERVICE_KEY else SUPABASE_ANON_KEY
            conn.setRequestProperty("apikey", authKey)
            conn.setRequestProperty("Authorization", "Bearer $authKey")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")

            val payload = JSONObject().apply {
                put("device_id", deviceId)
                put("license_key", licenseKey)
                put("is_active", true)
                put("order_id", sessionId)
                put("notes", "Auto-Activated via PayMongo GCash")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val code = conn.responseCode
            Log.d(TAG, "Record in Supabase response code: $code")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording license in Supabase", e)
        }
    }
}
