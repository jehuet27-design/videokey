package com.example.videokey.data

import android.util.Log
import java.io.File

/**
 * Parser for `shubidx` binary database format (SongHub / Philippine Karaoke).
 * Extracts song codes, titles, and artist associations.
 */
object ShubIdxParser {

    fun parse(idxFile: File): List<Song> {
        if (!idxFile.exists() || idxFile.length() < 1024) return emptyList()

        val songs = mutableListOf<Song>()
        try {
            val bytes = idxFile.readBytes()
            val total = bytes.size
            Log.d("ShubIdxParser", "Parsing shubidx (${bytes.size} bytes)...")

            var pos = 0x04FAF0.coerceAtMost(total - 16)
            while (pos < total - 12) {
                val start = pos
                while (pos < total - 12 && bytes[pos].toInt() in 32..126) {
                    pos++
                }
                val titleLen = pos - start
                if (titleLen in 2..80 && bytes[pos] == 0x01.toByte()) {
                    val title = String(bytes, start, titleLen, Charsets.ISO_8859_1).trim()
                    val count = bytes[pos + 1].toInt() and 0xFF
                    val code = readInt32LittleEndian(bytes, pos + 4)
                    val sectorOffset = (readInt32LittleEndian(bytes, pos + 8).toLong() and 0xFFFFFFFFL)

                    if (code in 1..999999 && !title.contains("WORDS") && !title.equals("Male", true) && !title.equals("Female", true)) {
                        val codeStr = String.format("%06d", code)
                        songs.add(
                            Song(
                                code = codeStr,
                                title = title,
                                artist = "Karaoke",
                                filePath = idxFile.absolutePath,
                                category = "OPM",
                                sectorOffset = sectorOffset,
                                sectorCount = if (count > 0) count else 32
                            )
                        )
                        pos += 12
                        continue
                    }
                }
                pos++
            }
        } catch (e: Exception) {
            Log.e("ShubIdxParser", "Error parsing shubidx", e)
        }

        Log.d("ShubIdxParser", "Total songs successfully parsed from shubidx: ${songs.size}")
        return songs
    }

    private fun readInt32LittleEndian(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 >= bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }
}
