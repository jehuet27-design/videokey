package com.example.videokey.data

import java.io.File
import java.util.concurrent.ConcurrentHashMap

class SongRepository {

    private val songs = mutableListOf<Song>()
    private val songsByCode = ConcurrentHashMap<String, Song>()

    init {
        // Songs start empty and are populated by scanning VideoKey storage (.vkpack or midi files)
    }

    fun getAllSongs(): List<Song> = synchronized(songs) { ArrayList(songs) }

    fun findByCode(code: String): Song? {
        val trimmed = code.trim()
        val direct = songsByCode[trimmed]
        if (direct != null) return direct

        val asInt = trimmed.toIntOrNull()
        if (asInt != null) {
            val padded6 = String.format("%06d", asInt)
            val matchPadded = songsByCode[padded6]
            if (matchPadded != null) return matchPadded
        }

        val unpadded = trimmed.trimStart('0')
        val matchUnpadded = songsByCode[unpadded]
        if (matchUnpadded != null) return matchUnpadded

        return synchronized(songs) {
            songs.firstOrNull { it.code.trimStart('0') == unpadded }
        }
    }

    fun getTotalSongsCount(): Int = synchronized(songs) { songs.size }

    fun getPlayableSongsCount(): Int = synchronized(songs) {
        songs.count { (it.isPacked && it.sectorOffset >= 0L) || (it.filePath.isNotEmpty() && !it.filePath.startsWith("http")) }
    }

    fun search(query: String, onlyPlayable: Boolean = false, isCodePlayable: (String) -> Boolean = { false }): List<Song> {
        val listSnapshot = synchronized(songs) { ArrayList(songs) }
        val stream = if (onlyPlayable) {
            listSnapshot.asSequence().filter { s ->
                s.isPacked || s.filePath.endsWith(".mid", true) || s.filePath.endsWith(".kar", true) || s.filePath.endsWith(".mp4", true) || s.filePath.endsWith(".mkv", true) || s.sectorOffset >= 0L || isCodePlayable(s.code)
            }
        } else {
            listSnapshot.asSequence()
        }

        if (query.isBlank()) return stream.take(100).toList()
        val q = query.trim().lowercase()
        return stream.filter {
            it.code.contains(q) ||
                    it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q)
        }.take(100).toList()
    }

    fun addSong(song: Song) {
        addSongs(listOf(song))
    }

    fun addSongs(newSongs: List<Song>) {
        synchronized(songs) {
            newSongs.forEach { raw ->
                val resolved = SongMetadataResolver.resolve(raw.code, raw.title, raw.artist)
                val it = raw.copy(title = resolved.title, artist = resolved.artist, category = resolved.category)
                val existing = songsByCode[it.code]
                if (existing == null) {
                    songs.add(it)
                    songsByCode[it.code] = it
                    val unpadded = it.code.trimStart('0')
                    if (unpadded.isNotEmpty()) songsByCode[unpadded] = it
                } else if (it.isPacked || it.filePath.endsWith(".mid", true) || it.filePath.endsWith(".kar", true) || it.filePath.endsWith(".mp4", true) || it.filePath.endsWith(".mkv", true)) {
                    val idx = songs.indexOfFirst { s -> s.code == it.code }
                    if (idx != -1) songs[idx] = it
                    songsByCode[it.code] = it
                    val unpadded = it.code.trimStart('0')
                    if (unpadded.isNotEmpty()) songsByCode[unpadded] = it
                }
            }
        }
    }

    fun setSongs(newSongs: List<Song>) {
        synchronized(songs) {
            songs.clear()
            songsByCode.clear()
            val resolvedList = newSongs.map { raw ->
                val resolved = SongMetadataResolver.resolve(raw.code, raw.title, raw.artist)
                raw.copy(title = resolved.title, artist = resolved.artist, category = resolved.category)
            }
            songs.addAll(resolvedList)
            resolvedList.forEach {
                songsByCode[it.code] = it
                val unpadded = it.code.trimStart('0')
                if (unpadded.isNotEmpty()) songsByCode[unpadded] = it
            }
        }
    }

    fun removeSong(song: Song): Boolean {
        synchronized(songs) {
            val removed = songs.removeIf { it.code == song.code || it.filePath == song.filePath }
            songsByCode.remove(song.code)
            val unpadded = song.code.trimStart('0')
            if (unpadded.isNotEmpty()) songsByCode.remove(unpadded)
            return removed
        }
    }


    fun scanFolder(folder: File, context: android.content.Context): StorageScanner.ScanResult {
        val scanner = StorageScanner(context)
        val result = scanner.scanSpecificFolder(folder)
        if (result.midiSongs.isNotEmpty()) {
            setSongs(result.midiSongs)
        }
        return result
    }

    fun scanInternalStorage(context: android.content.Context): StorageScanner.ScanResult {
        val scanner = StorageScanner(context)
        val result = scanner.autoScanInternalStorage()
        if (result.midiSongs.isNotEmpty()) {
            addSongs(result.midiSongs)
        }
        return result
    }

    fun scanDirectory(directory: File): Int {
        if (!directory.exists() || !directory.isDirectory) return 0
        var count = 0
        val pattern = Regex("""^(\d{4,6})\s*-\s*(.+?)\s*-\s*(.+?)\.(mid|kar)$""", RegexOption.IGNORE_CASE)

        directory.walkTopDown().maxDepth(2).forEach { file ->
            if (file.isFile && (file.extension.equals("mid", true) || file.extension.equals("kar", true))) {
                val match = pattern.find(file.name)
                val rawSong = if (match != null) {
                    val (code, title, artist) = match.destructured
                    Song(code.trim(), title.trim(), artist.trim(), file.absolutePath)
                } else {
                    val baseName = file.nameWithoutExtension
                    Song(String.format("%06d", count + 1), baseName, "", file.absolutePath)
                }
                val resolved = SongMetadataResolver.resolve(rawSong.code, rawSong.title, rawSong.artist)
                val song = rawSong.copy(title = resolved.title, artist = resolved.artist, category = resolved.category)

                synchronized(songs) {
                    songs.add(song)
                    songsByCode[song.code] = song
                    val unpadded = song.code.trimStart('0')
                    if (unpadded.isNotEmpty()) songsByCode[unpadded] = song
                }
                count++
            }
        }
        return count
    }
}
