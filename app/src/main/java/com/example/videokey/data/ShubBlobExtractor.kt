package com.example.videokey.data

import android.util.Log
import com.songhub.android.database.NativeLib
import java.io.File
import java.io.RandomAccessFile

object ShubBlobExtractor {

    private val defaultBlobDirs = listOf(
        File("/storage/emulated/0/VideoKey/idx"),
        File("/sdcard/VideoKey/idx"),
        File("/storage/emulated/0/SongHub/idx"),
        File("/sdcard/SongHub/idx"),
        File("/storage/emulated/0/VideoKey"),
        File("/sdcard/VideoKey")
    )

    fun extractMidi(
        sectorOffset: Long,
        sectorCount: Int,
        cacheDir: File,
        songCode: String,
        customBlobDirs: List<File> = emptyList()
    ): File? {
        val outFile = File(cacheDir, "${songCode}_extracted.mid")
        if (outFile.exists() && outFile.length() > 64) {
            Log.d("ShubBlobExtractor", "Using cached extracted MIDI for $songCode (${outFile.length()} bytes)")
            return outFile
        }

        if (sectorOffset < 0L) {
            Log.w("ShubBlobExtractor", "Invalid sectorOffset $sectorOffset for $songCode")
            return null
        }

        if (!NativeLib.isLoaded) {
            Log.w("ShubBlobExtractor", "NativeLib is not loaded")
            return null
        }

        val allDirs = (customBlobDirs + defaultBlobDirs).distinctBy { it.absolutePath }
        var targetFile: File? = null
        var relativeOffset = 0L

        val targetByteOffset = sectorOffset * 2048L
        val sectors = if (sectorCount in 1..128) sectorCount else 32
        val byteLength = sectors * 2048

        for (dir in allDirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            val containers = dir.listFiles { f ->
                f.isFile && (f.name.lowercase().startsWith("shubblob") || f.name.lowercase().startsWith("megmid"))
            }?.sortedBy { it.name }

            if (!containers.isNullOrEmpty()) {
                var accumulated = 0L
                for (c in containers) {
                    val next = accumulated + c.length()
                    if (targetByteOffset < next) {
                        targetFile = c
                        relativeOffset = targetByteOffset - accumulated
                        break
                    }
                    accumulated = next
                }
                if (targetFile != null) break
            }
        }

        if (targetFile == null || !targetFile.exists()) {
            Log.w("ShubBlobExtractor", "No container file found covering byte offset $targetByteOffset for $songCode")
            return null
        }

        try {
            val raw: ByteArray
            RandomAccessFile(targetFile, "r").use { raf ->
                val available = (raf.length() - relativeOffset).coerceAtLeast(0L)
                val readLen = available.coerceAtMost(byteLength.toLong()).toInt()
                if (readLen < 2048) {
                    Log.w("ShubBlobExtractor", "Not enough bytes ($readLen) at offset $relativeOffset for $songCode")
                    return null
                }
                raw = ByteArray(readLen)
                raf.seek(relativeOffset)
                raf.readFully(raw)
            }

            var processed = try {
                NativeLib.processMidiData(raw, false)
            } catch (t: Throwable) {
                Log.e("ShubBlobExtractor", "processMidiData(false) threw", t)
                null
            }

            if (processed == null || processed.isEmpty()) {
                Log.d("ShubBlobExtractor", "processMidiData(false) returned null/empty, trying processMidiData(true)...")
                processed = try {
                    NativeLib.processMidiData(raw, true)
                } catch (t: Throwable) {
                    Log.e("ShubBlobExtractor", "processMidiData(true) threw", t)
                    null
                }
            }

            if (processed != null && processed.size >= 14) {
                Log.d("ShubBlobExtractor", "processMidiData succeeded! Output size: ${processed.size} bytes, header: ${processed.take(4).map { "%02X".format(it) }}")
                // Check MThd header (0x4D 0x54 0x68 0x64)
                if (processed[0] == 0x4D.toByte() && processed[1] == 0x54.toByte() &&
                    processed[2] == 0x68.toByte() && processed[3] == 0x64.toByte()
                ) {
                    outFile.writeBytes(processed)
                    Log.d("ShubBlobExtractor", "Successfully extracted MIDI for $songCode (${processed.size} bytes) from ${targetFile.name}")
                    return outFile
                } else {
                    Log.w("ShubBlobExtractor", "Processed data header is not MThd: ${processed.take(4).map { "%02X".format(it) }}")
                }
            } else {
                Log.w("ShubBlobExtractor", "processMidiData returned null or empty for $songCode (processed: $processed)")
            }
        } catch (e: Exception) {
            Log.e("ShubBlobExtractor", "Extraction failed for $songCode from ${targetFile.name}", e)
        }
        return null
    }
}
