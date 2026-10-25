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
        // FAST SCAN: Exclusively scans the VideoKey folders on device storage (checking private hidden storage first)
        val vkCandidates = listOfNotNull(
            File(context.filesDir, "songs"),
            File(context.filesDir, "VideoKey/songs"),
            File(context.filesDir, "VideoKey"),
            context.filesDir,
            File("/storage/emulated/0/VideoKey"),
            File("/sdcard/VideoKey"),
            File(Environment.getExternalStorageDirectory(), "VideoKey"),
            context.getExternalFilesDir(null)?.let { File(it, "VideoKey") }
        ).distinctBy { it.absolutePath }

        val prefs = context.getSharedPreferences("videokey_prefs", Context.MODE_PRIVATE)
        val isVip = prefs.getBoolean("is_vip_activated", false)

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

            // 2. SoundFonts in VideoKey/SoundFont, VideoKey/soundfonts, Download, or external storage
            val extStorage = Environment.getExternalStorageDirectory()
            val sfDirs = listOfNotNull(
                File(vk, "SoundFont"), File(vk, "soundfonts"), vk,
                File(extStorage, "SoundFont"), File(extStorage, "soundfonts"),
                File(extStorage, "Download"), File(extStorage, "Download/SoundFont"),
                File(extStorage, "Music"),
                File(context.filesDir, "soundfonts"),
                context.getExternalFilesDir("soundfonts")
            ).distinctBy { it.absolutePath }
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
                    val companionBook = loadCompanionSongbook(dir)
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        val nameLower = file.name.lowercase()
                        if (file.absolutePath.contains("/backgrounds/", ignoreCase = true) ||
                            file.absolutePath.contains("\\backgrounds\\", ignoreCase = true) ||
                            file.parentFile?.name.equals("backgrounds", ignoreCase = true)) {
                            continue
                        }
                        if (VkPackReader.isPackFile(file)) {
                            val packSongs = VkPackReader.loadSongs(file)
                            for (song in packSongs) {
                                if (seenCodes.add(song.code)) foundSongs.add(song)
                            }
                        } else if (nameLower.endsWith(".mid") || nameLower.endsWith(".kar") || nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv")) {
                            val parsed = parseSongFilename(file, companionBook)
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

        // 0. Encrypted VideoKey packs (.vkpack) – prioritize private hidden storage
        val prefs = context.getSharedPreferences("videokey_prefs", Context.MODE_PRIVATE)
        val isVip = prefs.getBoolean("is_vip_activated", false)

        val packCandidateDirs = listOfNotNull(
            File(context.filesDir, "songs"),
            File(context.filesDir, "VideoKey/songs"),
            File(context.filesDir, "VideoKey"),
            context.filesDir,
            folder,
            File(folder, "songs"),
            File("/storage/emulated/0/VideoKey"),
            File("/storage/emulated/0/VideoKey/songs"),
            File("/sdcard/VideoKey"),
            File("/sdcard/VideoKey/songs"),
            File(context.getExternalFilesDir(null), "VideoKey"),
            File(context.getExternalFilesDir(null), "VideoKey/songs")
        ).distinct().filter { it.exists() && it.isDirectory }

        val allPacks = mutableListOf<File>()
        for (dir in packCandidateDirs) {
            val packs = dir.listFiles()?.filter { VkPackReader.isPackFile(it) } ?: emptyList()
            allPacks.addAll(packs)
        }
        val distinctPacks = allPacks.distinctBy { it.absolutePath }
        val selectedPack = distinctPacks.firstOrNull { it.name.contains("57k", ignoreCase = true) || it.name.contains("54k", ignoreCase = true) || it.length() > 550 * 1024 * 1024L }
            ?: distinctPacks.maxByOrNull { it.length() }
            ?: distinctPacks.firstOrNull()

        if (selectedPack != null) {
            val packSongs = VkPackReader.loadSongs(selectedPack)
            Log.d("StorageScanner", "Loaded ${packSongs.size} songs from primary pack ${selectedPack.absolutePath} (57k for all users)")
            for (song in packSongs) {
                if (seenCodes.add(song.code)) foundSongs.add(song)
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
            val companionBook = loadCompanionSongbook(folder)
            if (nameLower.endsWith(".mid") || nameLower.endsWith(".kar") || nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv")) {
                if (file.absolutePath.contains("/backgrounds/", ignoreCase = true) ||
                    file.absolutePath.contains("\\backgrounds\\", ignoreCase = true) ||
                    file.parentFile?.name.equals("backgrounds", ignoreCase = true)) {
                    continue
                }
                val parsed = parseSongFilename(file, companionBook)
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

    /**
     * Intelligently parses song code, title, and artist from filenames across all videoke conventions:
     * - "01234 - Title - Artist"
     * - "01234_Title_Artist"
     * - "01234 - Title"
     * - "Title - Artist"
     * - "Title (Artist)"
     * - "01234.mid"
     */
    fun parseSongFilename(file: File, companionBook: Map<String, Pair<String, String>> = emptyMap()): Song {
        val baseName = file.nameWithoutExtension.trim()
        val codeMatch = Regex("""^(\d{4,6})""").find(baseName)
        val extractedCode = codeMatch?.value

        // 1. Check companion songbook mapping if loaded (e.g. from songbook.txt / songs.csv)
        if (extractedCode != null && companionBook.containsKey(extractedCode)) {
            val (t, a) = companionBook[extractedCode]!!
            val resolved = SongMetadataResolver.resolve(extractedCode, t, a)
            return Song(extractedCode, resolved.title, resolved.artist, file.absolutePath, resolved.category)
        }

        var code = ""
        var rawTitle = ""
        var rawArtist = ""

        val threePartRegex = Regex("""^(\d{4,6})\s*[-_.]\s*(.+?)\s*[-_]\s*(.+?)$""")
        val threePartMatch = threePartRegex.find(baseName)

        if (threePartMatch != null) {
            val (c, t, a) = threePartMatch.destructured
            code = c.trim()
            rawTitle = t.trim()
            rawArtist = a.trim()
        } else if (baseName.contains(" - ")) {
            val parts = baseName.split(" - ", limit = 3)
            if (parts.size >= 3) {
                if (parts[0].trim().matches(Regex("""^\d{4,6}$"""))) {
                    code = parts[0].trim()
                    rawTitle = parts[1].trim()
                    rawArtist = parts[2].trim()
                } else {
                    rawArtist = parts[0].trim()
                    rawTitle = parts[1].trim()
                    code = String.format("%06d", Math.abs(baseName.hashCode() % 900000) + 100000)
                }
            } else if (parts.size == 2) {
                if (parts[0].trim().matches(Regex("""^\d{4,6}$"""))) {
                    code = parts[0].trim()
                    rawTitle = parts[1].trim()
                } else {
                    rawTitle = parts[0].trim()
                    rawArtist = parts[1].trim()
                    code = String.format("%06d", Math.abs(baseName.hashCode() % 900000) + 100000)
                }
            }
        } else if (baseName.contains("_") && !baseName.contains(" ")) {
            val parts = baseName.split("_")
            if (parts.size >= 3 && parts[0].matches(Regex("""^\d{4,6}$"""))) {
                code = parts[0].trim()
                rawTitle = parts[1].trim()
                rawArtist = parts.subList(2, parts.size).joinToString(" ").trim()
            } else if (parts.size == 2 && parts[0].matches(Regex("""^\d{4,6}$"""))) {
                code = parts[0].trim()
                rawTitle = parts[1].trim()
            } else if (parts.size == 2) {
                rawTitle = parts[0].trim()
                rawArtist = parts[1].trim()
                code = String.format("%06d", Math.abs(baseName.hashCode() % 900000) + 100000)
            }
        } else {
            val twoPartRegex = Regex("""^(\d{4,6})\s*[-_.:]\s*(.+?)$""")
            val twoPartMatch = twoPartRegex.find(baseName)
            if (twoPartMatch != null) {
                val (c, t) = twoPartMatch.destructured
                code = c.trim()
                rawTitle = t.trim()
            } else if (baseName.matches(Regex("""^\d{4,6}$"""))) {
                code = baseName
            } else {
                rawTitle = baseName
                code = String.format("%06d", Math.abs(baseName.hashCode() % 900000) + 100000)
            }
        }

        // Try extracting from MIDI header if artist is still missing
        if (!SongMetadataResolver.isValidArtist(rawArtist) && (file.extension.equals("mid", true) || file.extension.equals("kar", true))) {
            val midiMeta = extractMidiMetadata(file)
            if (midiMeta != null) {
                if (rawTitle.isEmpty() && midiMeta.first.isNotEmpty()) rawTitle = midiMeta.first
                if (midiMeta.second.isNotEmpty()) rawArtist = midiMeta.second
            }
        }

        val resolved = SongMetadataResolver.resolve(code, rawTitle, rawArtist)
        return Song(
            code = if (code.isNotEmpty()) code else String.format("%06d", Math.abs(baseName.hashCode() % 900000) + 100000),
            title = resolved.title,
            artist = resolved.artist,
            filePath = file.absolutePath,
            category = resolved.category
        )
    }

    private fun loadCompanionSongbook(dir: File): Map<String, Pair<String, String>> {
        val result = HashMap<String, Pair<String, String>>()
        val candidateNames = listOf("songbook.txt", "songs.txt", "songlist.txt", "songbook.csv", "songs.csv", "songlist.csv", "playlist.txt", "list.txt")
        val candidateFiles = candidateNames.mapNotNull { name ->
            val f = File(dir, name)
            if (f.exists() && f.isFile) f else null
        }
        for (f in candidateFiles) {
            try {
                f.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("//")) {
                        val parts = when {
                            trimmed.contains("|") -> trimmed.split("|")
                            trimmed.contains("\t") -> trimmed.split("\t")
                            trimmed.contains(",") -> trimmed.split(",")
                            trimmed.contains(" - ") -> trimmed.split(" - ")
                            else -> emptyList()
                        }.map { it.trim().trim('"', '\'') }

                        if (parts.size >= 3 && parts[0].matches(Regex("""^\d{1,6}$"""))) {
                            val code = String.format("%06d", parts[0].toInt())
                            result[code] = Pair(parts[1], parts[2])
                            result[parts[0]] = Pair(parts[1], parts[2])
                        } else if (parts.size == 2 && parts[0].matches(Regex("""^\d{1,6}$"""))) {
                            val code = String.format("%06d", parts[0].toInt())
                            result[code] = Pair(parts[1], "")
                            result[parts[0]] = Pair(parts[1], "")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("StorageScanner", "Could not parse companion songbook ${f.name}", e)
            }
        }
        return result
    }

    private fun extractMidiMetadata(file: File): Pair<String, String>? {
        if (!file.exists() || file.length() < 24) return null
        return try {
            val headerBytes = ByteArray(minOf(file.length(), 4096L).toInt())
            java.io.FileInputStream(file).use { it.read(headerBytes) }
            val text = String(headerBytes, Charsets.ISO_8859_1)

            var foundTitle = ""
            var foundArtist = ""

            val tMatches = Regex("""@T\s*([^@\r\n\x00]+)""", RegexOption.IGNORE_CASE).findAll(text).map { it.groupValues[1].trim() }.toList()
            if (tMatches.isNotEmpty()) {
                foundTitle = tMatches[0]
                if (tMatches.size > 1 && SongMetadataResolver.isValidArtist(tMatches[1])) {
                    foundArtist = tMatches[1]
                }
            }
            val aMatch = Regex("""@A\s*([^@\r\n\x00]+)""", RegexOption.IGNORE_CASE).find(text)
            if (aMatch != null) {
                val a = aMatch.groupValues[1].trim()
                if (SongMetadataResolver.isValidArtist(a)) {
                    foundArtist = a
                }
            }

            if (foundTitle.isEmpty() || foundArtist.isEmpty()) {
                val trackMatch = Regex("""\x00\xFF\x03[\x01-\x7F]([^\r\n\x00]+)""").find(text)
                if (trackMatch != null) {
                    val trackName = trackMatch.groupValues[1].trim()
                    if (trackName.contains(" - ")) {
                        val p = trackName.split(" - ", limit = 2)
                        if (foundTitle.isEmpty()) foundTitle = p[0].trim()
                        if (foundArtist.isEmpty() && SongMetadataResolver.isValidArtist(p[1])) foundArtist = p[1].trim()
                    }
                }
            }

            if (foundTitle.isNotEmpty() || foundArtist.isNotEmpty()) {
                Pair(foundTitle, foundArtist)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
