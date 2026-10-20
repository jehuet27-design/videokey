package com.example.videokey.data

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class YouTubeVideoItem(
    val id: String,
    val title: String,
    val uploader: String,
    val durationFormatted: String,
    val durationSeconds: Int,
    val thumbnailUrl: String
)

data class YouTubeDownloadProgress(
    val videoTitle: String,
    val percent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedKbps: Long = 0L,
    val statusText: String,
    val isCompleted: Boolean = false,
    val error: String? = null
)

/**
 * YouTube and Direct Video Stream Extractor and Downloader for VideoKey:
 * - Downloads YouTube karaoke tracks directly to the songs directory with automatic 6-digit numbering.
 * - Downloads video backgrounds directly into VideoKey/backgrounds for visual accompaniment.
 * - Uses resilient public stream endpoints with automatic fallback.
 */
class YouTubeKaraokeDownloader(private val context: Context) {

    companion object {
        private const val TAG = "YouTubeDownloader"
        private val PIPED_INSTANCES = listOf(
            "https://api.piped.private.coffee",
            "https://pipedapi.kavin.rocks",
            "https://piped-api.lunar.icu"
        )
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeDownloadJob: Job? = null

    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
    }

    fun getSongsFolder(): File {
        val root = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
        val dir = File(root, "VideoKey/songs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getBackgroundsFolder(): File {
        val root = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
        val dir = File(root, "VideoKey/backgrounds")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        val urlRegex = Regex("""(?:https?://)?(?:www\.|m\.)?(?:youtube\.com/(?:watch\?.*v=|shorts/|live/|embed/)|youtu\.be/)([a-zA-Z0-9_-]{11})""")
        val match = urlRegex.find(trimmed)
        if (match != null) {
            return match.groupValues[1]
        }
        if (trimmed.length == 11 && trimmed.matches(Regex("""[a-zA-Z0-9_-]{11}"""))) {
            return trimmed
        }
        return null
    }

    /**
     * Search YouTube for karaoke songs or background videos using Piped API
     */
    suspend fun search(query: String): List<YouTubeVideoItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val directVideoId = extractVideoId(query)
        if (directVideoId != null) {
            // User pasted a YouTube URL or direct Video ID!
            for (instance in PIPED_INSTANCES) {
                try {
                    val streamApiUrl = "$instance/streams/$directVideoId"
                    val conn = (URL(streamApiUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android) VideoKey/2.0")
                        setRequestProperty("Accept", "application/json")
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        val json = JSONObject(body)
                        val title = json.optString("title", "YouTube Karaoke Video")
                        val uploader = json.optString("uploader", "YouTube")
                        val durationSec = json.optInt("duration", 0)
                        val mins = durationSec / 60
                        val secs = durationSec % 60
                        val durFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)
                        val thumb = json.optString("thumbnailUrl", "https://i.ytimg.com/vi/$directVideoId/hqdefault.jpg")

                        return@withContext listOf(
                            YouTubeVideoItem(
                                id = directVideoId,
                                title = title,
                                uploader = uploader,
                                durationFormatted = durFormatted,
                                durationSeconds = durationSec,
                                thumbnailUrl = thumb
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Direct video fetch from $instance failed: ${e.message}")
                }
            }

            // Fallback direct item so user can still play/download
            return@withContext listOf(
                YouTubeVideoItem(
                    id = directVideoId,
                    title = "YouTube Video ($directVideoId)",
                    uploader = "YouTube Direct",
                    durationFormatted = "00:00",
                    durationSeconds = 0,
                    thumbnailUrl = "https://i.ytimg.com/vi/$directVideoId/hqdefault.jpg"
                )
            )
        }

        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        for (instance in PIPED_INSTANCES) {
            try {
                val searchUrl = "$instance/search?q=$encodedQuery&filter=videos"
                val conn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) VideoKey/2.0")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val json = JSONObject(body)
                    val items = json.optJSONArray("items") ?: continue
                    val results = mutableListOf<YouTubeVideoItem>()

                    for (i in 0 until minOf(items.length(), 20)) {
                        val item = items.optJSONObject(i) ?: continue
                        val urlStr = item.optString("url", "")
                        val videoId = if (urlStr.contains("v=")) {
                            urlStr.substringAfter("v=").substringBefore("&")
                        } else if (urlStr.startsWith("/watch?v=")) {
                            urlStr.removePrefix("/watch?v=").substringBefore("&")
                        } else {
                            ""
                        }
                        if (videoId.isBlank()) continue

                        val title = item.optString("title", "Karaoke Video")
                        val uploader = item.optString("uploaderName", "YouTube")
                        val durationSec = item.optInt("duration", 0)
                        val mins = durationSec / 60
                        val secs = durationSec % 60
                        val durFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)
                        val thumb = item.optString("thumbnail", "")

                        results.add(
                            YouTubeVideoItem(
                                id = videoId,
                                title = title,
                                uploader = uploader,
                                durationFormatted = durFormatted,
                                durationSeconds = durationSec,
                                thumbnailUrl = thumb
                            )
                        )
                    }
                    if (results.isNotEmpty()) {
                        return@withContext results
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search instance $instance failed: ${e.message}")
            }
        }
        emptyList()
    }

    /**
     * Resolves a direct 1080p (or 720p fallback) MP4 download URL from YouTube video ID.
     */
    suspend fun resolve1080pStreamUrl(
        videoId: String,
        onStatusUpdate: ((String, Int) -> Unit)? = null
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val formats = listOf("1080", "720")
        for (fmt in formats) {
            try {
                onStatusUpdate?.invoke("Kinukuha ang ${fmt}p MP4 stream...", 3)
                val initUrl = "https://p.savenow.to/api/v2/download?button=1&format=$fmt&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D$videoId"
                val conn = (URL(initUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 12000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) VideoKey/2.0")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val json = JSONObject(body)
                    val rawTitle = json.optString("title", json.optJSONObject("info")?.optString("title", "Karaoke Video"))
                    val directUrl = json.optString("url", "").ifBlank { json.optString("download_url", "") }
                    if (directUrl.isNotBlank() && directUrl.startsWith("http")) {
                        onStatusUpdate?.invoke("Nahanap ang stream! Nagsisimula...", 14)
                        return@withContext Pair(directUrl, rawTitle)
                    }

                    val progressUrl = json.optString("progress_url", "").ifBlank {
                        val id = json.optString("id", "")
                        if (id.isNotBlank()) "https://p.savenow.to/api/progress?id=$id" else ""
                    }

                    if (progressUrl.isNotBlank()) {
                        for (attempt in 1..22) {
                            delay(1400)
                            try {
                                val pConn = (URL(progressUrl).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 8000
                                    readTimeout = 8000
                                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) VideoKey/2.0")
                                    setRequestProperty("Accept", "application/json")
                                }
                                if (pConn.responseCode == 200) {
                                    val pBody = pConn.inputStream.bufferedReader().use { it.readText() }
                                    pConn.disconnect()
                                    val pJson = JSONObject(pBody)
                                    val success = pJson.optInt("success", 0)
                                    val dlUrl = pJson.optString("download_url", "")
                                    val pTitle = pJson.optString("title", rawTitle)
                                    val textStatus = pJson.optString("text", "Inihahanda...")
                                    val pollPercent = (3 + attempt * 1).coerceAtMost(14)
                                    onStatusUpdate?.invoke("${fmt}p MP4: $textStatus", pollPercent)

                                    if (success == 1 && dlUrl.isNotBlank() && dlUrl.startsWith("http")) {
                                        Log.i(TAG, "Successfully resolved ${fmt}p MP4: $dlUrl")
                                        onStatusUpdate?.invoke("Handa na ang ${fmt}p stream...", 15)
                                        return@withContext Pair(dlUrl, pTitle)
                                    }
                                }
                            } catch (pe: Exception) {
                                Log.w(TAG, "Poll attempt $attempt warning: ${pe.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Format $fmt resolution error: ${e.message}")
            }
        }
        null
    }

    /**
     * Resolves a direct MP4 streaming URL from a video ID or direct link
     */
    suspend fun resolveStreamUrl(
        videoUrlOrId: String,
        onStatusUpdate: ((String, Int) -> Unit)? = null
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val trimmed = videoUrlOrId.trim()

        // 1. If it's already a direct video file link (.mp4 / .mkv)
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            if (trimmed.endsWith(".mp4", ignoreCase = true) || trimmed.endsWith(".mkv", ignoreCase = true)) {
                val cleanName = trimmed.substringAfterLast("/").substringBefore("?")
                return@withContext Pair(trimmed, cleanName)
            }
        }

        // 2. Extract YouTube video ID
        val videoId = when {
            trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
            trimmed.contains("watch?v=") -> trimmed.substringAfter("watch?v=").substringBefore("&")
            trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$")) -> trimmed
            else -> ""
        }

        if (videoId.isBlank()) return@withContext null

        // Primary: 1080p MP4 Dedicated Extractor
        val resolved1080 = resolve1080pStreamUrl(videoId, onStatusUpdate)
        if (resolved1080 != null) {
            return@withContext resolved1080
        }

        // Secondary fallback: Piped instances
        for (instance in PIPED_INSTANCES) {
            try {
                onStatusUpdate?.invoke("Sinusuri ang fallback stream...", 6)
                val streamApiUrl = "$instance/streams/$videoId"
                val conn = (URL(streamApiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) VideoKey/2.0")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val json = JSONObject(body)
                    val title = json.optString("title", "Karaoke Video")
                    val videoStreams = json.optJSONArray("videoStreams") ?: continue

                    var bestUrl: String? = null
                    var bestQuality = 0

                    for (i in 0 until videoStreams.length()) {
                        val vs = videoStreams.optJSONObject(i) ?: continue
                        val isVideoOnly = vs.optBoolean("videoOnly", false)
                        val streamUrl = vs.optString("url", "")

                        if (!isVideoOnly && streamUrl.isNotBlank()) {
                            val qualityStr = vs.optString("quality", "0")
                            val qNum = qualityStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 360
                            if (bestUrl == null || (qNum in 360..1080 && qNum > bestQuality)) {
                                bestUrl = streamUrl
                                bestQuality = qNum
                            }
                        }
                    }

                    if (bestUrl != null) {
                        return@withContext Pair(bestUrl, title)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Stream resolution instance $instance failed: ${e.message}")
            }
        }
        null
    }

    /**
     * Download YouTube Karaoke Video into the songs folder with an assigned 6-digit number
     */
    fun downloadAsSong(
        item: YouTubeVideoItem,
        existingMaxCode: Int,
        onProgress: (YouTubeDownloadProgress) -> Unit,
        onFinished: (Song?) -> Unit
    ) {
        cancelDownload()
        activeDownloadJob = scope.launch {
            onProgress(
                YouTubeDownloadProgress(
                    videoTitle = item.title,
                    percent = 0,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    statusText = "Kinukuha ang video stream..."
                )
            )

            val resolved = resolveStreamUrl(item.id) { statusMsg, prepPct ->
                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = item.title,
                        percent = prepPct,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        statusText = statusMsg
                    )
                )
            }
            if (resolved == null) {
                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = item.title,
                        percent = 0,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        statusText = "Bigo: Hindi ma-access ang video stream.",
                        error = "Hindi ma-download ang video stream."
                    )
                )
                onFinished(null)
                return@launch
            }

            val (streamUrl, streamTitle) = resolved
            val nextCode = (existingMaxCode + 1).coerceAtLeast(900001)
            val codeStr = nextCode.toString().padStart(6, '0')

            // Parse title & artist cleanly
            val rawTitle = if (streamTitle.isNotBlank()) streamTitle else item.title
            val cleanTitleParts = parseTitleAndArtist(rawTitle, item.uploader)
            val safeFileName = "$codeStr - ${cleanTitleParts.first} - ${cleanTitleParts.second}.mp4"
                .replace(Regex("""[/\\:*?"<>|]"""), "")

            val targetFile = File(getSongsFolder(), safeFileName)
            val tempFile = File(context.cacheDir, "yt_temp_${System.currentTimeMillis()}.mp4")

            val success = downloadStreamToFile(streamUrl, tempFile, rawTitle, onProgress)
            if (success && tempFile.exists() && tempFile.length() > 1024L) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()

                val createdSong = Song(
                    code = codeStr,
                    title = cleanTitleParts.first,
                    artist = cleanTitleParts.second,
                    filePath = targetFile.absolutePath
                )

                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = rawTitle,
                        percent = 100,
                        downloadedBytes = targetFile.length(),
                        totalBytes = targetFile.length(),
                        statusText = "✓ Tapos na! Naidagdag sa Songbook bilang #$codeStr",
                        isCompleted = true
                    )
                )
                onFinished(createdSong)
            } else {
                tempFile.delete()
                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = rawTitle,
                        percent = 0,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        statusText = "Bigo ang pag-download.",
                        error = "Hindi natapos ang pag-download."
                    )
                )
                onFinished(null)
            }
        }
    }

    /**
     * Download YouTube Video as a Stage Video Background
     */
    fun downloadAsBackground(
        item: YouTubeVideoItem,
        onProgress: (YouTubeDownloadProgress) -> Unit,
        onFinished: (File?) -> Unit
    ) {
        cancelDownload()
        activeDownloadJob = scope.launch {
            onProgress(
                YouTubeDownloadProgress(
                    videoTitle = item.title,
                    percent = 0,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    statusText = "Kinukuha ang background stream..."
                )
            )

            val resolved = resolveStreamUrl(item.id) { statusMsg, prepPct ->
                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = item.title,
                        percent = prepPct,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        statusText = statusMsg
                    )
                )
            }
            if (resolved == null) {
                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = item.title,
                        percent = 0,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        statusText = "Bigo: Hindi ma-access ang video stream.",
                        error = "Hindi ma-download ang video stream."
                    )
                )
                onFinished(null)
                return@launch
            }

            val (streamUrl, streamTitle) = resolved
            val rawTitle = if (streamTitle.isNotBlank()) streamTitle else item.title
            val safeName = rawTitle.replace(Regex("""[/\\:*?"<>|]"""), "").trim().take(40) + ".mp4"
            val targetFile = File(getBackgroundsFolder(), safeName)
            val tempFile = File(context.cacheDir, "yt_bg_${System.currentTimeMillis()}.mp4")

            val success = downloadStreamToFile(streamUrl, tempFile, rawTitle, onProgress)
            if (success && tempFile.exists() && tempFile.length() > 1024L) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()

                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = rawTitle,
                        percent = 100,
                        downloadedBytes = targetFile.length(),
                        totalBytes = targetFile.length(),
                        statusText = "✓ Tapos na! Naidagdag sa Video Backgrounds",
                        isCompleted = true
                    )
                )
                onFinished(targetFile)
            } else {
                tempFile.delete()
                onProgress(
                    YouTubeDownloadProgress(
                        videoTitle = rawTitle,
                        percent = 0,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        statusText = "Bigo ang pag-download.",
                        error = "Hindi natapos ang pag-download."
                    )
                )
                onFinished(null)
            }
        }
    }

    private fun downloadStreamToFile(
        streamUrl: String,
        destFile: File,
        title: String,
        onProgress: (YouTubeDownloadProgress) -> Unit
    ): Boolean {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var conn: HttpURLConnection? = null

        return try {
            conn = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) VideoKey/2.0")
                instanceFollowRedirects = true
            }

            val rawTotalBytes = conn.contentLengthLong
            val totalBytes = if (rawTotalBytes > 0) rawTotalBytes else -1L
            inputStream = conn.inputStream
            outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(65536)
            var bytesRead: Int
            var totalDownloaded = 0L
            var lastUpdate = System.currentTimeMillis()
            var lastDownloaded = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdate >= 300L) {
                    val deltaBytes = totalDownloaded - lastDownloaded
                    val deltaTimeSec = (now - lastUpdate) / 1000.0
                    val speedKbps = if (deltaTimeSec > 0) ((deltaBytes / 1024.0) / deltaTimeSec).toLong() else 0L

                    val mbDownloaded = totalDownloaded / (1024.0 * 1024.0)
                    val speedStr = if (speedKbps >= 1024) {
                        String.format(Locale.US, "%.1f MB/s", speedKbps / 1024.0)
                    } else {
                        "$speedKbps KB/s"
                    }

                    val pct: Int
                    val statusText: String
                    if (totalBytes > 0) {
                        val mbTotal = totalBytes / (1024.0 * 1024.0)
                        pct = ((totalDownloaded * 100) / totalBytes).toInt().coerceIn(1, 99)
                        statusText = "Dina-download: ${String.format(Locale.US, "%.1f", mbDownloaded)} MB / ${String.format(Locale.US, "%.1f", mbTotal)} MB ($speedStr)"
                    } else {
                        // Dynamic asymptotic progress calculation when Content-Length is omitted (chunked stream)
                        val estProgress = (15 + (80 * (1.0 - kotlin.math.exp(-mbDownloaded / 25.0)))).toInt().coerceIn(15, 96)
                        pct = estProgress
                        statusText = "Dina-download: ${String.format(Locale.US, "%.1f", mbDownloaded)} MB ($speedStr)"
                    }

                    onProgress(
                        YouTubeDownloadProgress(
                            videoTitle = title,
                            percent = pct,
                            downloadedBytes = totalDownloaded,
                            totalBytes = totalBytes,
                            speedKbps = speedKbps,
                            statusText = statusText
                        )
                    )
                    lastUpdate = now
                    lastDownloaded = totalDownloaded
                }
            }

            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun parseTitleAndArtist(fullTitle: String, defaultArtist: String): Pair<String, String> {
        val clean = fullTitle
            .replace(Regex("""(?i)\(?(HD|Official|Karaoke|Karaoke Version|Minus One|No Vocal|Instrumental|Lyrics?|Video)\)?"""), "")
            .replace(Regex("""[\[\]()]"""), "")
            .trim()

        val split = clean.split("-", limit = 2)
        return if (split.size == 2) {
            val part1 = split[0].trim()
            val part2 = split[1].trim()
            // If part 1 looks like title and part 2 artist, or vice versa
            Pair(part1, part2.ifBlank { defaultArtist })
        } else {
            Pair(clean.ifBlank { "Karaoke Song" }, defaultArtist.ifBlank { "YouTube" })
        }
    }
}
