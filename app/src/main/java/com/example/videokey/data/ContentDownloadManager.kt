package com.example.videokey.data

import android.content.Context
import android.util.Log
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

    // Base target storage directory
    fun getBaseStorageDir(): File {
        val root = File("/storage/emulated/0/VideoKey")
        if (root.exists() || root.mkdirs()) return root
        val fallback = File(context.getExternalFilesDir(null), "VideoKey")
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }

    // Catalog of available content packs
    fun getAvailableCatalog(): List<DownloadItem> {
        val releaseBase = "https://github.com/jehuet27-design/videokey/releases/download/v2.0"
        return listOf(
            // 1. Core Song Pack
            DownloadItem(
                id = "core_songs",
                title = "🎵 Philippine Videoke Songs Pack",
                category = ContentCategory.SONGS,
                description = "Kumpletong koleksyon ng libu-libong OPM, Pop, Rock, at Classic videoke songs.",
                sizeFormatted = "496 MB",
                downloadUrl = "$releaseBase/videokey_songs.vkpack",
                relativeDestinationPath = "videokey_songs.vkpack",
                checkFileRelativePath = "videokey_songs.vkpack"
            ),
            // 2. High-Definition SoundFont
            DownloadItem(
                id = "hd_soundfont",
                title = "🎛️ Platinum Major HD SoundFont",
                category = ContentCategory.SOUNDFONT,
                description = "High-definition videoke soundbank na may buong tunog ng gitara, drums, at percussions.",
                sizeFormatted = "98 MB",
                downloadUrl = "$releaseBase/songhub.sf2",
                relativeDestinationPath = "SoundFont/songhub.sf2",
                checkFileRelativePath = "SoundFont/songhub.sf2"
            ),
            // 3. Background Videos: 3D Dance Pack (User selectable)
            DownloadItem(
                id = "bg_3d_dance",
                title = "🕺 3D Dance Videoke Stage",
                category = ContentCategory.BACKGROUND_VIDEO,
                description = "Sumasayaw na 3D animation videoke video backgrounds (sayaw1 & sayaw2).",
                sizeFormatted = "56 MB",
                downloadUrl = "$releaseBase/bg_3d_dance.zip",
                relativeDestinationPath = "backgrounds/bgv/3D",
                isZip = true,
                checkFileRelativePath = "backgrounds/bgv/3D/sayaw1.mp4"
            ),
            // 4. Background Videos: Motion Graphics & Stage Lights (User selectable)
            DownloadItem(
                id = "bg_motion",
                title = "🌀 Motion Graphics & Stage Lights",
                category = ContentCategory.BACKGROUND_VIDEO,
                description = "Modern motion loops, concert lights, at electronic stage effects (motion 1-4).",
                sizeFormatted = "87 MB",
                downloadUrl = "$releaseBase/bg_motion.zip",
                relativeDestinationPath = "backgrounds/bgv/Motion",
                isZip = true,
                checkFileRelativePath = "backgrounds/bgv/Motion/motion1.mp4"
            ),
            // 5. Background Videos: Nature Landscapes & Scenic Pack (User selectable)
            DownloadItem(
                id = "bg_nature",
                title = "🌊 Nature & Scenic Landscapes",
                category = ContentCategory.BACKGROUND_VIDEO,
                description = "Magagandang tanawin ng dagat, bundok, ulap, at kalikasan (Video 1-4).",
                sizeFormatted = "228 MB",
                downloadUrl = "$releaseBase/bg_nature.zip",
                relativeDestinationPath = "backgrounds/bgv/Nature",
                isZip = true,
                checkFileRelativePath = "backgrounds/bgv/Nature/Video1.mp4"
            ),
            // 6. Background Wallpapers / HD Photos (User selectable)
            DownloadItem(
                id = "bg_photos",
                title = "🖼️ Classic Photo Stage Wallpapers",
                category = ContentCategory.BACKGROUND_IMAGE,
                description = "Magaan at mabilis na classic videoke photo backgrounds.",
                sizeFormatted = "2.6 MB",
                downloadUrl = "$releaseBase/bg_photos.zip",
                relativeDestinationPath = "backgrounds",
                isZip = true,
                checkFileRelativePath = "backgrounds/1.jpg"
            )
        )
    }

    /**
     * Checks if a specific downloadable item is already installed on the phone.
     */
    fun isItemInstalled(item: DownloadItem): Boolean {
        val base = getBaseStorageDir()
        val target = File(base, item.checkFileRelativePath)
        return target.exists() && target.length() > 0
    }

    /**
     * Checks if any songs exist in the base folder.
     */
    fun hasAnySongsInstalled(): Boolean {
        val base = getBaseStorageDir()
        val vkPack = File(base, "videokey_songs.vkpack")
        if (vkPack.exists() && vkPack.length() > 1024) return true
        val songsDir = File(base, "songs")
        if (songsDir.exists() && (songsDir.listFiles()?.isNotEmpty() == true)) return true
        return false
    }

    /**
     * Starts downloading the selected content item.
     */
    fun downloadItem(item: DownloadItem, onFinish: () -> Unit = {}) {
        currentDownloadJob?.cancel()
        currentDownloadJob = scope.launch {
            _downloadStatus.value = DownloadStatus.Downloading(
                itemId = item.id,
                percent = 0,
                downloadedBytes = 0L,
                totalBytes = 0L
            )

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
                            }
                        }
                        output.flush()
                    }
                }
                currentConn.disconnect()

                // If ZIP file, extract automatically to target directory
                if (item.isZip) {
                    _downloadStatus.value = DownloadStatus.Extracting(item.id)
                    val targetExtractDir = File(baseDir, item.relativeDestinationPath).apply { mkdirs() }
                    extractZip(destFile, targetExtractDir)
                    destFile.delete()
                }

                _downloadStatus.value = DownloadStatus.Completed(item.id, "Matagumpay na na-download ang ${item.title}!")
                withContext(Dispatchers.Main) {
                    onFinish()
                }
            } catch (e: CancellationException) {
                Log.d("ContentDownloadManager", "Download cancelled: ${item.id}")
                destFile.delete()
                _downloadStatus.value = DownloadStatus.Idle
            } catch (e: Exception) {
                Log.e("ContentDownloadManager", "Download failed for ${item.id}", e)
                destFile.delete()
                _downloadStatus.value = DownloadStatus.Error(
                    itemId = item.id,
                    errorMessage = "Hindi natapos ang download: ${e.message ?: "Walang koneksyon"}"
                )
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
    }

    fun dismissStatus() {
        _downloadStatus.value = DownloadStatus.Idle
    }
}
