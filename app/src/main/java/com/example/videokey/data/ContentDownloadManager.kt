package com.example.videokey.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.videokey.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Manages downloadable content (Songs, SoundFonts, and Selectable Background Packs)
 * - Users can pick and choose which backgrounds to download to save storage.
 * - Downloads directly into the app's VideoKey storage.
 * - Live real-time progress tracking (MB & %).
 * - Background persistence via WakeLock & System Notifications so download is never interrupted when minimized.
 */
class ContentDownloadManager(private val context: Context) {

    enum class ContentCategory {
        SONGS,
        SOUNDFONT,
        BACKGROUND_VIDEO,
        BACKGROUND_IMAGE
    }

    data class DownloadItem(
        val id: String,
        val title: String,
        val category: ContentCategory,
        val description: String,
        val sizeFormatted: String,
        val downloadUrl: String,
        val relativeDestinationPath: String, // e.g. "backgrounds/bgv/3D/sayaw1.mp4" or "videokey_songs.vkpack"
        val isZip: Boolean = false,
        val checkFileRelativePath: String // Path to check if already installed
    )

    sealed class DownloadStatus {
        object Idle : DownloadStatus()
        data class Downloading(
            val itemId: String,
            val percent: Int,
            val downloadedBytes: Long,
            val totalBytes: Long,
            val speedKbps: Long = 0L
        ) : DownloadStatus()
        data class Extracting(val itemId: String) : DownloadStatus()
        data class Completed(val itemId: String, val message: String) : DownloadStatus()
        data class Error(val itemId: String, val errorMessage: String) : DownloadStatus()
    }

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentDownloadJob: Job? = null

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val NOTIFICATION_ID = 2001
    private val CHANNEL_ID = "videokey_content_downloads"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VideoKey Content Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status ng pag-download ng mga kanta, SoundFont, at backgrounds"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(title: String, text: String, progress: Int = -1, ongoing: Boolean = true) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)

            if (progress in 0..100) {
                builder.setProgress(100, progress, false)
            } else if (ongoing) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(0, 0, false)
            }

            notificationManager?.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e("ContentDownloadManager", "Failed to show download notification", e)
        }
    }

    private fun cancelNotification() {
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    // Base target storage directory
    fun getBaseStorageDir(): File {
        val root = File("/storage/emulated/0/VideoKey")
        if (root.exists() || root.mkdirs()) return root
        val sdcard = File("/sdcard/VideoKey")
        if (sdcard.exists() || sdcard.mkdirs()) return sdcard
        val fallback = File(context.getExternalFilesDir(null), "VideoKey")
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }

    // Catalog of available content packs
    fun getAvailableCatalog(): List<DownloadItem> {
        val releaseBase = "https://github.com/jehuet27-design/videokey/releases/download/content-packs"
        return listOf(
            // 1. Videoke Songs Pack (Removed "Philippine")
            DownloadItem(
                id = "core_songs",
                title = "🎵 Videoke Songs Pack",
                category = ContentCategory.SONGS,
                description = "Kumpletong koleksyon ng libu-libong OPM, Pop, Rock, at Classic videoke songs.",
                sizeFormatted = "496 MB",
                downloadUrl = "$releaseBase/videokey_songs.vkpack",
                relativeDestinationPath = "videokey_songs.vkpack",
                checkFileRelativePath = "videokey_songs.vkpack"
            ),
            // 2. Complete SoundFont Bank (All-in-one single download)
            DownloadItem(
                id = "all_soundfonts",
                title = "🎛️ Complete SoundFont Bank",
                category = ContentCategory.SOUNDFONT,
                description = "Lahat ng SoundFonts (VideoKey Studio Minus-One, Dream SAM2634 v2.1, at MegaPro) ay mabilis at magaan ma-download.",
                sizeFormatted = "345 MB",
                downloadUrl = "$releaseBase/soundfonts_all.zip",
                relativeDestinationPath = "SoundFont",
                isZip = true,
                checkFileRelativePath = "SoundFont/VideoKey_Studio_Minus1.sf2"
            ),
            // 3. Complete Video Backgrounds Pack (All-in-one single download)
            DownloadItem(
                id = "all_backgrounds",
                title = "🎬 Complete Drone Shoot Video Pack",
                category = ContentCategory.BACKGROUND_VIDEO,
                description = "Magandang HD Drone Shoot nature background video para sa videoke stage.",
                sizeFormatted = "204 MB",
                downloadUrl = "$releaseBase/backgrounds_all.zip",
                relativeDestinationPath = "backgrounds",
                isZip = true,
                checkFileRelativePath = "backgrounds/DRONE_SHOOT.mp4"
            )
        )
    }

    /**
     * Checks if a specific downloadable item is already installed on the phone.
     */
    fun isItemInstalled(item: DownloadItem): Boolean {
        return when (item.id) {
            "core_songs" -> hasAnySongsInstalled()
            "all_soundfonts" -> isSoundFontInstalled()
            "all_backgrounds" -> isBackgroundVideoInstalled()
            else -> {
                val base = getBaseStorageDir()
                val target = File(base, item.checkFileRelativePath)
                if (target.exists() && target.length() > 0) return true
                val inSongs = File(base, "songs/${item.checkFileRelativePath}")
                if (inSongs.exists() && inSongs.length() > 0) return true
                val inSoundfonts = File(base, "soundfonts/${File(item.checkFileRelativePath).name}")
                if (inSoundfonts.exists() && inSoundfonts.length() > 0) return true
                val inSoundFontCap = File(base, "SoundFont/${File(item.checkFileRelativePath).name}")
                if (inSoundFontCap.exists() && inSoundFontCap.length() > 0) return true
                val inBackgrounds = File(base, "backgrounds/${File(item.checkFileRelativePath).name}")
                if (inBackgrounds.exists() && inBackgrounds.length() > 0) return true
                val altTarget = File("/storage/emulated/0/SongHub", item.checkFileRelativePath)
                if (altTarget.exists() && altTarget.length() > 0) return true
                val altSongs = File("/storage/emulated/0/SongHub/songs/${item.checkFileRelativePath}")
                altSongs.exists() && altSongs.length() > 0
            }
        }
    }

    /**
     * Checks if any songs exist in the base folder, songs subfolder, or SongHub directory.
     */
    fun hasAnySongsInstalled(): Boolean {
        val base = getBaseStorageDir()
        val checkFiles = listOf(
            File(base, "videokey_songs.vkpack"),
            File(base, "songs/videokey_songs.vkpack"),
            File("/storage/emulated/0/VideoKey/videokey_songs.vkpack"),
            File("/storage/emulated/0/VideoKey/songs/videokey_songs.vkpack"),
            File("/sdcard/VideoKey/videokey_songs.vkpack"),
            File("/sdcard/VideoKey/songs/videokey_songs.vkpack"),
            File(context.getExternalFilesDir(null), "VideoKey/videokey_songs.vkpack"),
            File(context.getExternalFilesDir(null), "VideoKey/songs/videokey_songs.vkpack"),
            File(context.filesDir, "VideoKey/videokey_songs.vkpack"),
            File("/storage/emulated/0/SongHub/videokey_songs.vkpack"),
            File("/storage/emulated/0/SongHub/songs/videokey_songs.vkpack")
        )
        if (checkFiles.any { it.exists() && it.length() > 1024 }) return true

        val songsDirs = listOf(
            File(base, "songs"),
            File(base, ""),
            File("/storage/emulated/0/VideoKey/songs"),
            File("/storage/emulated/0/VideoKey"),
            File("/sdcard/VideoKey/songs"),
            File("/sdcard/VideoKey"),
            File(context.getExternalFilesDir(null), "VideoKey/songs"),
            File(context.getExternalFilesDir(null), "VideoKey"),
            File(context.filesDir, "VideoKey/songs"),
            File("/storage/emulated/0/SongHub/songs"),
            File("/storage/emulated/0/SongHub/blob")
        )
        for (dir in songsDirs) {
            try {
                if (dir.exists() && (dir.listFiles()?.any {
                    val n = it.name.lowercase()
                    (n.endsWith(".vkpack") || n.endsWith(".mid") || n.endsWith(".kar") || n.endsWith(".mp4") || n.endsWith(".mkv")) && it.length() > 1024
                } == true)) {
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Checks if VideoKey Studio Minus-One, Dream SAM2634, MegaPro SoundFont, or any valid .sf2 is installed.
     */
    fun isSoundFontInstalled(): Boolean {
        val base = getBaseStorageDir()
        val sfDirs = listOf(
            File(base, "SoundFont"),
            File(base, "soundfonts"),
            File("/storage/emulated/0/VideoKey/SoundFont"),
            File("/storage/emulated/0/VideoKey/soundfonts"),
            File("/sdcard/VideoKey/SoundFont"),
            File("/sdcard/VideoKey/soundfonts"),
            File(context.getExternalFilesDir(null), "VideoKey/SoundFont"),
            File(context.getExternalFilesDir(null), "VideoKey/soundfonts"),
            File(context.filesDir, "SoundFont"),
            File("/storage/emulated/0/SongHub/SoundFont"),
            File("/storage/emulated/0/SongHub/soundfonts")
        )
        for (dir in sfDirs) {
            try {
                if (dir.exists() && (dir.listFiles()?.any { it.name.endsWith(".sf2", ignoreCase = true) && it.length() > 1024 } == true)) {
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Checks if background video (e.g. Drone Shoot or any mp4/mkv video in backgrounds/videos folder) is installed.
     */
    fun isBackgroundVideoInstalled(): Boolean {
        val base = getBaseStorageDir()
        val bgDirs = listOf(
            File(base, "backgrounds"),
            File(base, "videos"),
            File("/storage/emulated/0/VideoKey/backgrounds"),
            File("/storage/emulated/0/VideoKey/videos"),
            File("/sdcard/VideoKey/backgrounds"),
            File("/sdcard/VideoKey/videos"),
            File(context.getExternalFilesDir(null), "VideoKey/backgrounds"),
            File(context.getExternalFilesDir(null), "VideoKey/videos"),
            File(context.filesDir, "backgrounds"),
            File("/storage/emulated/0/SongHub/backgrounds"),
            File("/storage/emulated/0/SongHub/bgv")
        )
        for (dir in bgDirs) {
            try {
                if (dir.exists() && (dir.listFiles()?.any { 
                    (it.name.endsWith(".mp4", ignoreCase = true) || it.name.endsWith(".mkv", ignoreCase = true)) && it.length() > 1024 
                } == true)) {
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Starts downloading the selected content item.
     */
    fun downloadItem(item: DownloadItem, onFinish: (Boolean) -> Unit = {}) {
        currentDownloadJob?.cancel()
        currentDownloadJob = scope.launch {
            _downloadStatus.value = DownloadStatus.Downloading(
                itemId = item.id,
                percent = 0,
                downloadedBytes = 0L,
                totalBytes = 0L
            )

            try {
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VideoKey:DownloadLock")
                wakeLock?.acquire(60 * 60 * 1000L) // 1-hour max safety limit
            } catch (e: Exception) {
                Log.w("ContentDownloadManager", "WakeLock acquire failed", e)
            }
            updateNotification("VideoKey: Nagda-download...", item.title, 0, ongoing = true)

            val baseDir = getBaseStorageDir()
            val destFile = if (item.isZip) {
                File(context.cacheDir, "${item.id}_temp.zip")
            } else {
                File(baseDir, item.relativeDestinationPath).apply {
                    parentFile?.mkdirs()
                }
            }

            try {
                val url = URL(item.downloadUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "VideoKey-Android-App/1.0")
                }

                // Handle HTTP redirects (GitHub releases redirect to AWS S3/objects)
                var redirectCode = conn.responseCode
                var currentConn = conn
                var redirectCount = 0
                while ((redirectCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        redirectCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        redirectCode == 307 || redirectCode == 308) && redirectCount < 5
                ) {
                    val newUrl = currentConn.getHeaderField("Location")
                    currentConn.disconnect()
                    currentConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 20000
                        setRequestProperty("User-Agent", "VideoKey-Android-App/1.0")
                    }
                    redirectCode = currentConn.responseCode
                    redirectCount++
                }

                val contentLength = currentConn.contentLength.toLong()
                var downloaded = 0L
                var lastProgressUpdate = 0L
                var lastNotificationUpdate = 0L

                currentConn.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(32768)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive()
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 150) {
                                lastProgressUpdate = now
                                val percent = if (contentLength > 0) {
                                    ((downloaded * 100) / contentLength).toInt().coerceIn(0, 99)
                                } else 0
                                _downloadStatus.value = DownloadStatus.Downloading(
                                    itemId = item.id,
                                    percent = percent,
                                    downloadedBytes = downloaded,
                                    totalBytes = contentLength
                                )

                                if (now - lastNotificationUpdate > 1000) {
                                    lastNotificationUpdate = now
                                    val mbDl = downloaded / (1024f * 1024f)
                                    val mbTot = contentLength / (1024f * 1024f)
                                    val text = "${String.format("%.1f", mbDl)} MB / ${String.format("%.1f", mbTot)} MB ($percent%)"
                                    updateNotification("Dina-download: ${item.title}", text, percent, ongoing = true)
                                }
                            }
                        }
                        output.flush()
                    }
                }
                currentConn.disconnect()

                // If ZIP file, extract automatically to target directory
                if (item.isZip) {
                    _downloadStatus.value = DownloadStatus.Extracting(item.id)
                    updateNotification("Ina-unpack: ${item.title}", "Inaayos ang mga files para sa videoke...", -1, ongoing = true)
                    val targetExtractDir = File(baseDir, item.relativeDestinationPath).apply { mkdirs() }
                    extractZip(destFile, targetExtractDir)
                    destFile.delete()
                }

                _downloadStatus.value = DownloadStatus.Completed(item.id, "Matagumpay na na-download ang ${item.title}!")
                updateNotification("✓ Tapos na ang download!", item.title, ongoing = false)
                scope.launch {
                    delay(5000)
                    cancelNotification()
                }

                withContext(Dispatchers.Main) {
                    onFinish(true)
                }
            } catch (e: CancellationException) {
                Log.d("ContentDownloadManager", "Download cancelled: ${item.id}")
                destFile.delete()
                _downloadStatus.value = DownloadStatus.Idle
                cancelNotification()
                withContext(Dispatchers.Main) {
                    onFinish(false)
                }
            } catch (e: Exception) {
                Log.e("ContentDownloadManager", "Download failed for ${item.id}", e)
                destFile.delete()
                _downloadStatus.value = DownloadStatus.Error(
                    itemId = item.id,
                    errorMessage = "Hindi natapos ang download: ${e.message ?: "Walang koneksyon"}"
                )
                updateNotification("✕ Hindi natapos ang download", item.title, ongoing = false)
                withContext(Dispatchers.Main) {
                    onFinish(false)
                }
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun cancelCurrentDownload() {
        currentDownloadJob?.cancel()
        _downloadStatus.value = DownloadStatus.Idle
        cancelNotification()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    fun dismissStatus() {
        _downloadStatus.value = DownloadStatus.Idle
    }
}
