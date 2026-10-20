package com.example.videokey.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Automatically scans the phone's Internal Storage for:
 * 1. IDX files (`shubidx`, `shubblob`)
 * 2. SoundFonts (`*.sf2`)
 * 3. MIDI Karaoke files (`*.mid`, `*.kar`)
 */
class StorageScanner(private val context: Context) {

    data class ScanResult(
        val idxFile: File? = null,
        val blobFile: File? = null,
        val soundFonts: List<File> = emptyList(),
        val midiSongs: List<Song> = emptyList(),
        val totalSongsLoaded: Int = 0
    )

    fun autoScanInternalStorage(): ScanResult {
        // FAST SCAN: Exclusively scans the VideoKey folders on device storage
        val vkCandidates = listOfNotNull(
            File("/storage/emulated/0/VideoKey"),
            File("/sdcard/VideoKey"),
            File(Environment.getExternalStorageDirectory(), "VideoKey"),
            context.getExternalFilesDir(null)?.let { File(it, "VideoKey") },
            File(context.filesDir, "VideoKey")
        ).distinctBy { it.absolutePath }

        var foundIdx: File? = null
        var foundBlob: File? = null
        val foundSf2 = mutableListOf<File>()
        val foundSongs = mutableListOf<Song>()
        val seenCodes = HashSet<String>()

        for (vk in vkCandidates) {
            if (!vk.exists() || !vk.isDirectory) continue
            Log.d("StorageScanner", "Fast scanning VideoKey folder: ${vk.absolutePath}")

            // 1. Check IDX in VideoKey/idx or VideoKey root
            val idxDirs = listOf(File(vk, "idx"), vk)
            for (dir in idxDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        val nameLower = file.name.lowercase()
                        if (foundIdx == null && (nameLower == "shubidx" || nameLower.startsWith("shubidx") || nameLower.endsWith(".idx"))) {
                            foundIdx = file
                            Log.d("StorageScanner", "Found IDX file at: ${file.absolutePath}")
                        }
                        if (foundBlob == null && (nameLower == "shubblob" || nameLower.endsWith(".blob") || nameLower.endsWith(".dat"))) {
                            foundBlob = file
                            Log.d("StorageScanner", "Found BLOB file at: ${file.absolutePath}")
                        }
                    }
                }
            }

            // 2. SoundFonts in VideoKey/SoundFont or VideoKey/soundfonts
            val sfDirs = listOf(File(vk, "SoundFont"), File(vk, "soundfonts"), vk)
            for (dir in sfDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        if (file.name.lowercase().endsWith(".sf2") && !foundSf2.any { it.name == file.name }) {
                            foundSf2.add(file)
                            Log.d("StorageScanner", "Found SoundFont at: ${file.absolutePath}")
                        }
                    }
                }
            }

            // 3. Songs in VideoKey/songs or VideoKey root (.vkpack and midi files)
            val songDirs = listOf(File(vk, "songs"), vk)
            for (dir in songDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        val nameLower = file.name.lowercase()
                        if (file.absolutePath.contains("/backgrounds/", ignoreCase = true) ||
                            file.absolutePath.contains("\\backgrounds\\", ignoreCase = true) ||
                            file.parentFile?.name.equals("backgrounds", ignoreCase = true)) {
                            continue
                        }
                        if (VkPackReader.isPackFile(file)) {
                            for (song in VkPackReader.loadSongs(file)) {
                                if (seenCodes.add(song.code)) foundSongs.add(song)
                            }
                        } else if (nameLower.endsWith(".mid") || nameLower.endsWith(".kar") || nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv")) {
                            val parsed = parseSongFilename(file)
                            if (seenCodes.add(parsed.code)) {
                                foundSongs.add(parsed)
                            }
                        }
                    }
                }
            }
        }

        // 4. Parse shubidx to add songs to songbook
        if (foundIdx != null) {
            val idxSongs = ShubIdxParser.parse(foundIdx!!)
            Log.d("StorageScanner", "Loaded ${idxSongs.size} songs from ${foundIdx!!.name}")
            for (song in idxSongs) {
                if (seenCodes.add(song.code)) {
                    foundSongs.add(song)
                }
            }
        }

        Log.d("StorageScanner", "Fast scan complete: ${foundSongs.size} total songs, ${foundSf2.size} SoundFonts")

        return ScanResult(
            idxFile = foundIdx,
            blobFile = foundBlob,
            soundFonts = foundSf2,
            midiSongs = foundSongs,
            totalSongsLoaded = foundSongs.size
        )
    }

    fun scanSpecificFolder(folder: File): ScanResult {
        if (!folder.exists() || !folder.isDirectory) {
            Log.w("StorageScanner", "scanSpecificFolder: Directory does not exist: ${folder.absolutePath}")
            return ScanResult()
        }

        var foundIdx: File? = null
        var foundBlob: File? = null
        val foundSf2 = mutableListOf<File>()
        val foundSongs = mutableListOf<Song>()
        val seenCodes = HashSet<String>()

        val allFiles = try {
            folder.walkTopDown().maxDepth(2).filter { it.isFile && it.length() > 0L }.toList()
        } catch (e: Exception) {
            folder.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()
        }

        // 0. Encrypted VideoKey packs (.vkpack) – strictly inside VideoKey directories
        val packCandidateDirs = listOfNotNull(
            folder,
            File(folder, "songs"),
            File("/storage/emulated/0/VideoKey"),
            File("/storage/emulated/0/VideoKey/songs"),
            File("/sdcard/VideoKey"),
            File("/sdcard/VideoKey/songs"),
            File(context.getExternalFilesDir(null), "VideoKey"),
            File(context.getExternalFilesDir(null), "VideoKey/songs")
        ).distinct().filter { it.exists() && it.isDirectory }

        for (dir in packCandidateDirs) {
            val packs = dir.listFiles()?.filter { VkPackReader.isPackFile(it) } ?: emptyList()
            for (file in packs) {
                val packSongs = VkPackReader.loadSongs(file)
                Log.d("StorageScanner", "Loaded ${packSongs.size} songs from pack ${file.absolutePath}")
                for (song in packSongs) {
                    if (seenCodes.add(song.code)) foundSongs.add(song)
                }
            }
        }

        for (file in allFiles) {
            val nameLower = file.name.lowercase()
            // 1. IDX
            if (foundIdx == null && (nameLower == "shubidx" || nameLower.startsWith("shubidx") || nameLower.endsWith(".idx"))) {
                foundIdx = file
                Log.d("StorageScanner", "Specific folder found IDX: ${file.absolutePath}")
            }
            // 2. BLOB
            if (foundBlob == null && (nameLower == "shubblob" || nameLower.endsWith(".blob") || nameLower.endsWith(".dat"))) {
                foundBlob = file
                Log.d("StorageScanner", "Specific folder found BLOB: ${file.absolutePath}")
            }
            // 3. SoundFont
            if (nameLower.endsWith(".sf2") && !foundSf2.any { it.name == file.name }) {
                foundSf2.add(file)
                Log.d("StorageScanner", "Specific folder found SF2: ${file.absolutePath}")
            }
            // 4. MIDI and MP4 Video Karaoke songs (Exclude background videos)
            if (nameLower.endsWith(".mid") || nameLower.endsWith(".kar") || nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv")) {
                if (file.absolutePath.contains("/backgrounds/", ignoreCase = true) ||
                    file.absolutePath.contains("\\backgrounds\\", ignoreCase = true) ||
                    file.parentFile?.name.equals("backgrounds", ignoreCase = true)) {
                    continue
                }
                val parsed = parseSongFilename(file)
                if (seenCodes.add(parsed.code)) {
                    foundSongs.add(parsed)
                }
            }
        }

        // Parse IDX if present
        if (foundIdx != null) {
            val idxSongs = ShubIdxParser.parse(foundIdx)
            Log.d("StorageScanner", "Specific folder parsed ${idxSongs.size} songs from ${foundIdx.name}")
            for (song in idxSongs) {
                if (seenCodes.add(song.code)) {
                    foundSongs.add(song)
                }
            }
        }

        return ScanResult(
            idxFile = foundIdx,
            blobFile = foundBlob,
            soundFonts = foundSf2,
            midiSongs = foundSongs,
            totalSongsLoaded = foundSongs.size
        )
    }

    fun parseSongFilename(file: File): Song {
        val baseName = file.nameWithoutExtension
        val parts = baseName.split(" - ", limit = 3)
        return when {
            parts.size >= 3 -> Song(parts[0].trim(), parts[1].trim(), parts[2].trim(), file.absolutePath)
            parts.size == 2 -> Song(parts[0].trim(), parts[1].trim(), "Karaoke", file.absolutePath)
            else -> {
                val pattern = Regex("""^(\d{4,6})\s*-\s*(.+?)\s*-\s*(.+?)$""", RegexOption.IGNORE_CASE)
                val match = pattern.find(baseName)
                if (match != null) {
                    val (c, t, a) = match.destructured
                    Song(c.trim(), t.trim(), a.trim(), file.absolutePath)
                } else {
                    Song(baseName.take(6), baseName, "Unknown", file.absolutePath)
                }
            }
        }
    }
}
