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
        val rootPaths = listOf(
            Environment.getExternalStorageDirectory(),
            File("/storage/emulated/0"),
            File("/sdcard")
        ).distinctBy { it.absolutePath }

        var foundIdx: File? = null
        var foundBlob: File? = null
        val foundSf2 = mutableListOf<File>()
        val foundSongs = mutableListOf<Song>()

        for (root in rootPaths) {
            if (!root.exists()) continue
            Log.d("StorageScanner", "Scanning root directory: ${root.absolutePath}")

            // 1. Direct subfolder checks for VideoKey and SongHub IDX
            val idxDirs = listOf(
                File(root, "VideoKey/idx"),
                File(root, "VideoKey"),
                File(root, "SongHub/idx"),
                File(root, "SongHub"),
                File(root, "idx"),
                File(root, "Music/idx"),
                File(root, "Music"),
                File(root, "Download/idx"),
                File(root, "Download")
            )

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

            // 2. Direct SoundFont checks
            val sfDirs = listOf(
                File(root, "VideoKey/SoundFont"),
                File(root, "VideoKey"),
                File(root, "SongHub/sound"),
                File(root, "SongHub"),
                File(root, "SoundFont"),
                File(root, "soundfonts"),
                File(root, "Music"),
                File(root, "Download")
            )

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

            // 3. Direct MIDI songs checks
            val songDirs = listOf(
                File(root, "VideoKey/songs"),
                File(root, "Music/songs"),
                File(root, "VideoKey"),
                File(root, "Music"),
                File(root, "Download")
            )

            val seenCodes = HashSet<String>()
            for (dir in songDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        val nameLower = file.name.lowercase()
                        if (VkPackReader.isPackFile(file)) {
                            for (song in VkPackReader.loadSongs(file)) {
                                if (seenCodes.add(song.code)) foundSongs.add(song)
                            }
                        } else if (nameLower.endsWith(".mid") || nameLower.endsWith(".kar")) {
                            val parsed = parseSongFilename(file)
                            if (seenCodes.add(parsed.code)) {
                                foundSongs.add(parsed)
                            }
                        }
                    }
                }
            }

            if (foundSongs.isNotEmpty() && foundSf2.isNotEmpty()) {
                break
            }
        }

        // 4. Parse shubidx to add songs to songbook
        if (foundIdx != null) {
            val idxSongs = ShubIdxParser.parse(foundIdx!!)
            Log.d("StorageScanner", "Loaded ${idxSongs.size} songs from ${foundIdx!!.name}")
            val existingCodes = HashSet<String>(foundSongs.map { it.code })
            for (song in idxSongs) {
                if (existingCodes.add(song.code)) {
                    foundSongs.add(song)
                }
            }
        }

        Log.d("StorageScanner", "Scan complete: ${foundSongs.size} total playable songs, ${foundSf2.size} SoundFonts")

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
            folder.walkTopDown().maxDepth(3).filter { it.isFile && it.length() > 0L }.toList()
        } catch (e: Exception) {
            folder.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()
        }

        // 0. Encrypted VideoKey packs (.vkpack) – loaded first so they take priority
        val packCandidateDirs = listOfNotNull(
            folder,
            folder.parentFile,
            File(folder, "songs"),
            File("/storage/emulated/0/VideoKey"),
            File("/sdcard/VideoKey")
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
            // 4. MIDI songs
            if (nameLower.endsWith(".mid") || nameLower.endsWith(".kar")) {
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

    private fun parseSongFilename(file: File): Song {
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
