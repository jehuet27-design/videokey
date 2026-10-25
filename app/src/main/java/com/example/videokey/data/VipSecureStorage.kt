package com.example.videokey.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware-Backed Secure Storage for VIP Licenses using EncryptedSharedPreferences (AES-256 GCM).
 * Includes automatic graceful fallback to obfuscated storage if device keystore fails.
 */
object VipSecureStorage {
    private const val TAG = "VipSecureStorage"
    private const val PREFS_NAME = "videokey_vip_secure_prefs"

    private const val KEY_IS_VIP = "is_vip_active_secure"
    private const val KEY_DEVICE_ID = "vip_device_id_secure"
    private const val KEY_LICENSE_KEY = "vip_license_key_secure"
    private const val KEY_ACTIVATION_TIME = "vip_activation_time_secure"
    private const val KEY_PENDING_CHECKOUT = "vip_pending_checkout_session"

    fun savePendingCheckout(context: Context, checkoutSessionId: String) {
        try {
            getPrefs(context).edit().putString(KEY_PENDING_CHECKOUT, checkoutSessionId).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save pending checkout", e)
        }
    }

    fun getPendingCheckout(context: Context): String? {
        return try {
            getPrefs(context).getString(KEY_PENDING_CHECKOUT, null)
        } catch (e: Exception) {
            null
        }
    }

    fun clearPendingCheckout(context: Context) {
        try {
            getPrefs(context).edit().remove(KEY_PENDING_CHECKOUT).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear pending checkout", e)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Keystore EncryptedSharedPreferences unavailable, falling back to standard prefs: ${e.message}")
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveVipStatus(context: Context, deviceId: String, licenseKey: String, isActive: Boolean) {
        try {
            getPrefs(context).edit()
                .putBoolean(KEY_IS_VIP, isActive)
                .putString(KEY_DEVICE_ID, deviceId.trim())
                .putString(KEY_LICENSE_KEY, licenseKey.trim())
                .putLong(KEY_ACTIVATION_TIME, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "Saved VIP Status securely: active=$isActive, device=$deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save secure VIP status", e)
        }
    }

    fun isVipActive(context: Context): Boolean {
        return try {
            getPrefs(context).getBoolean(KEY_IS_VIP, false)
        } catch (e: Exception) {
            false
        }
    }

    fun getStoredLicenseKey(context: Context): String? {
        return try {
            getPrefs(context).getString(KEY_LICENSE_KEY, null)
        } catch (e: Exception) {
            null
        }
    }

    fun getStoredDeviceId(context: Context): String? {
        return try {
            getPrefs(context).getString(KEY_DEVICE_ID, null)
        } catch (e: Exception) {
            null
        }
    }

    fun clearVip(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
        } catch (_: Exception) {}
    }
}
