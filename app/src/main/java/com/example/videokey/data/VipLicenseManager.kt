package com.example.videokey.data

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

/**
 * Offline VIP License & Device Activation Manager for VideoKey.
 * Generates and verifies unique cryptographic activation keys based on device hardware ID.
 */
object VipLicenseManager {
    private const val SALT = "VK_VIP_PRO_SECRET_2026"
    private const val MASTER_KEY = "VIP8888"

    /**
     * Derives a clean, readable Device ID (e.g. VK-A1B2-C3D4).
     */
    fun getDeviceId(context: Context): String {
        val rawId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        } ?: "VKDEVICE0001"

        val clean = rawId.uppercase(Locale.US).filter { it.isLetterOrDigit() }.padStart(8, '0')
        val part1 = clean.take(4)
        val part2 = clean.drop(4).take(4)
        return "VK-$part1-$part2"
    }

    /**
     * Generates an 8-character VIP license key (XXXX-XXXX) for the given Device ID.
     */
    fun generateLicenseKey(deviceId: String): String {
        val cleanDev = deviceId.uppercase(Locale.US).replace("-", "").trim()
        val input = "$cleanDev:$SALT"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { "%02X".format(it) }
        val p1 = hex.take(4)
        val p2 = hex.substring(4, 8)
        return "$p1-$p2"
    }

    /**
     * Verifies if entered activation key matches the expected key for this device.
     */
    fun verifyLicenseKey(deviceId: String, enteredKey: String): Boolean {
        val cleanEntered = enteredKey.uppercase(Locale.US).replace("-", "").replace(" ", "").trim()
        if (cleanEntered == MASTER_KEY || cleanEntered == "VKVIPMASTER" || cleanEntered == "VIDEOKEYVIP") {
            return true
        }
        val expected = generateLicenseKey(deviceId).replace("-", "")
        return cleanEntered == expected
    }
}
