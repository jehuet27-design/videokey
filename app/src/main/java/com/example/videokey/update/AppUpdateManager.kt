package com.example.videokey.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Universal Over-The-Air (OTA) Force-Update Engine:
 * - Checks for newer versions over ANY connection (Wi-Fi, mobile data, hotspot, across the internet)
 * - Automatically forces download when a newer version is released
 * - Live real-time download progress (percentage, MB transferred)
 * - Directly triggers Android package installer with FileProvider
 */
class AppUpdateManager(private val context: Context) {

    data class AppUpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val isForceUpdate: Boolean,
        val releaseNotes: String
    )

    sealed class UpdateState {
        object Idle : UpdateState()
        data class Checking(val message: String) : UpdateState()
        data class UpToDate(val currentVersion: String) : UpdateState()
        data class UpdateAvailable(val info: AppUpdateInfo) : UpdateState()
        data class Downloading(
            val info: AppUpdateInfo,
            val progressPercent: Int,
            val downloadedBytes: Long,
            val totalBytes: Long
        ) : UpdateState()
        data class ReadyToInstall(val info: AppUpdateInfo, val apkFile: File) : UpdateState()
        data class Error(val message: String, val canRetry: Boolean = true) : UpdateState()
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    companion object {
        const val PREF_KEY_CUSTOM_UPDATE_URL = "custom_update_manifest_url"
        // Default online repository manifest (works across ANY internet connection worldwide)
        const val DEFAULT_ONLINE_MANIFEST_URL = "https://raw.githubusercontent.com/jehuet27-design/videokey/main/version.json"
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Checks for updates from the configured manifest URL.
     * If [autoDownloadIfNewer] is true, immediately starts force-downloading!
     * If [isManual] is false, fails silently to avoid interrupting karaoke if offline or no update.
     */
    fun checkForUpdates(
        manifestUrlOverride: String? = null,
        autoDownloadIfNewer: Boolean = true,
        isManual: Boolean = false
    ) {
        scope.launch {
            if (isManual) {
                _updateState.value = UpdateState.Checking("Sinusuri ang pinakabagong update...")
            }
            val prefs = context.getSharedPreferences("videokey_settings", Context.MODE_PRIVATE)
            val urlString = manifestUrlOverride
                ?: prefs.getString(PREF_KEY_CUSTOM_UPDATE_URL, null)
                ?: DEFAULT_ONLINE_MANIFEST_URL

            try {
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 10000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "VideoKey-Android-App/${getCurrentVersionName()}")
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val json = JSONObject(jsonText)
                    val remoteVersionCode = json.optInt("versionCode", 0)
                    val remoteVersionName = json.optString("versionName", "1.0")
                    val apkUrl = json.optString("apkUrl", "")
                    val isForce = json.optBoolean("forceUpdate", false)
                    val notes = json.optString("releaseNotes", "Mahahalagang pagpapahusay sa sistema.")

                    val currentCode = getCurrentVersionCode()
                    Log.d("AppUpdateManager", "Version check: current=$currentCode, remote=$remoteVersionCode, url=$apkUrl")

                    if (remoteVersionCode > currentCode && apkUrl.isNotBlank()) {
                        val info = AppUpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = remoteVersionName,
                            apkUrl = apkUrl,
                            isForceUpdate = isForce,
                            releaseNotes = notes
                        )
                        _updateState.value = UpdateState.UpdateAvailable(info)
                        if (isForce || autoDownloadIfNewer) {
                            startDownload(info)
                        }
                    } else {
                        if (isManual) {
                            _updateState.value = UpdateState.UpToDate(getCurrentVersionName())
                        } else {
                            _updateState.value = UpdateState.Idle
                        }
                    }
                } else {
                    conn.disconnect()
                    if (isManual) {
                        _updateState.value = UpdateState.Error("Hindi makakonekta sa server ($responseCode)")
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.w("AppUpdateManager", "Update check failed: ${e.message}")
                if (isManual) {
                    _updateState.value = UpdateState.Error("Walang internet o hindi ma-access ang server.")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    /**
     * Downloads the APK file with real-time byte & percent tracking.
     */
    fun startDownload(info: AppUpdateInfo) {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            _updateState.value = UpdateState.Downloading(
                info = info,
                progressPercent = 0,
                downloadedBytes = 0L,
                totalBytes = 0L
            )

            try {
                val url = URL(info.apkUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "VideoKey-Android-App/${getCurrentVersionName()}")
                }

                if (conn.responseCode !in 200..299) {
                    _updateState.value = UpdateState.Error("Bigo ang pag-download: HTTP ${conn.responseCode}")
                    conn.disconnect()
                    return@launch
                }

                val totalLength = conn.contentLength.toLong()
                val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                targetDir.mkdirs()
                val apkFile = File(targetDir, "VideoKey_v${info.versionName}.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var totalDownloaded = 0L
                        var lastReportTime = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) {
                                apkFile.delete()
                                return@launch
                            }
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 200 || totalDownloaded == totalLength) {
                                lastReportTime = now
                                val pct = if (totalLength > 0) ((totalDownloaded * 100) / totalLength).toInt().coerceIn(0, 100) else 0
                                _updateState.value = UpdateState.Downloading(
                                    info = info,
                                    progressPercent = pct,
                                    downloadedBytes = totalDownloaded,
                                    totalBytes = totalLength
                                )
                            }
                        }
                    }
                }
                conn.disconnect()

                Log.d("AppUpdateManager", "APK Download complete: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
                _updateState.value = UpdateState.ReadyToInstall(info, apkFile)

                // Prompt user with Android Package Installer immediately!
                withContext(Dispatchers.Main) {
                    promptInstall(apkFile)
                }

            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error downloading APK", e)
                _updateState.value = UpdateState.Error("Naputol ang pag-download: ${e.message}")
            }
        }
    }

    /**
     * Launches Android's PackageInstaller to install the downloaded APK.
     */
    fun promptInstall(apkFile: File) {
        if (!apkFile.exists()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
            _updateState.value = UpdateState.Idle
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Failed to launch package installer", e)
            _updateState.value = UpdateState.Error("Hindi mabuksan ang installer: ${e.message}")
        }
    }

    fun dismiss() {
        downloadJob?.cancel()
        _updateState.value = UpdateState.Idle
    }
}
