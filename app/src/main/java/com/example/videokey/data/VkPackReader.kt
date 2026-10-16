package com.example.videokey.data

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Reader for VideoKey's proprietary encrypted song archive (`.vkpack`), produced by
 * `tools/VkPackBuilder.java`.
 *
 * - All songs live inside ONE file (Deflate-compressed, lossless).
 * - Every song and the index table are AES-128-CTR encrypted, so the archive cannot be opened
 *   by WinRAR / 7-Zip / media players.
 * - Opening a pack only decrypts the small index (~50 ms for 48k songs). Playing a song seeks
 *   straight to its blob, decrypts + inflates only that song (a few ms) and verifies CRC32.
 *
 * IMPORTANT: [SECRET_OBF] must match VkPackBuilder.java.
 */
object VkPackReader {

    private const val TAG = "VkPackReader"
    private const val HEADER_SIZE = 48
    const val EXTENSION = "vkpack"

    private val SECRET_OBF = intArrayOf(
        0x0C, 0x11, 0x79, 0x68, 0x6A, 0x68, 0x6C, 0x7B, 0x0A, 0x28, 0x35, 0x11, 0x3B, 0x28, 0x3B,
        0x35, 0x31, 0x3F, 0x1A, 0x10, 0x3F, 0x32, 0x2F, 0x3F, 0x24, 0x0A, 0x3B, 0x39, 0x31
    )

    private class PackInfo(val file: File, val nonce: ByteArray, val key: ByteArray)

    /** Opened packs keyed by absolute path (only header + key are cached, not song data). */
    private val packs = ConcurrentHashMap<String, PackInfo>()

    fun isPackFile(file: File): Boolean = file.isFile && file.extension.equals(EXTENSION, ignoreCase = true)

    private fun secret(): ByteArray = ByteArray(SECRET_OBF.size) { (SECRET_OBF[it] xor 0x5A).toByte() }

    private fun deriveKey(nonce: ByteArray): ByteArray {
        val sha = MessageDigest.getInstance("SHA-256")
        sha.update(secret())
        sha.update(nonce)
        return sha.digest().copyOf(16)
    }

    private fun ivFor(nonce: ByteArray, id: Long): ByteArray {
        val iv = ByteArray(16)
        val v = id + 1
        for (i in 0 until 8) iv[i] = (nonce[i].toInt() xor (v ushr (56 - 8 * i)).toInt()).toByte()
        System.arraycopy(nonce, 8, iv, 8, 4)
        return iv
    }

    private fun ctr(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/CTR/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return c.doFinal(data)
    }

    private fun inflate(data: ByteArray, origLen: Int): ByteArray {
        val inf = Inflater(true)
        try {
            inf.setInput(data)
            val out = ByteArray(origLen)
            var n = 0
            while (n < origLen && !inf.finished()) {
                val r = inf.inflate(out, n, origLen - n)
                if (r == 0 && (inf.needsInput() || inf.needsDictionary())) break
                n += r
            }
            if (n != origLen) throw IllegalStateException("Inflate size mismatch $n != $origLen")
            return out
        } finally {
            inf.end()
        }
    }

    private fun crc(data: ByteArray): Int = CRC32().apply { update(data) }.value.toInt()

    /** Opens a pack and returns all songs it contains (fast: only the index is decrypted). */
    fun loadSongs(file: File): List<Song> {
        val t0 = System.nanoTime()
        try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(HEADER_SIZE)
                raf.readFully(header)
                val h = DataInputStream(ByteArrayInputStream(header))
                val magic = ByteArray(4).also { h.readFully(it) }
                if (String(magic, Charsets.US_ASCII) != "VKPK") {
                    Log.w(TAG, "Not a VKPK file: ${file.absolutePath}")
                    return emptyList()
                }
                h.readUnsignedByte() // version
                h.readUnsignedByte() // flags
                h.readShort()
                val count = h.readInt()
                val indexOffset = h.readLong()
                val idxComp = h.readInt()
                val idxOrig = h.readInt()
                val nonce = ByteArray(16).also { h.readFully(it) }
                val headerCrc = h.readInt()
                if (headerCrc != crc(header.copyOf(44))) {
                    Log.w(TAG, "Header CRC mismatch: ${file.absolutePath}")
                    return emptyList()
                }
                val key = deriveKey(nonce)

                val idxEnc = ByteArray(idxComp)
                raf.seek(indexOffset)
                raf.readFully(idxEnc)
                val idxRaw = inflate(ctr(key, ivFor(nonce, -1), idxEnc), idxOrig)
                val d = DataInputStream(ByteArrayInputStream(idxRaw))

                val path = file.absolutePath
                val songs = ArrayList<Song>(count)
                for (i in 0 until count) {
                    val code = d.readUTF()
                    val title = d.readUTF()
                    val artist = d.readUTF()
                    val offset = d.readLong()
                    val compLen = d.readInt()
                    val origLen = d.readInt()
                    val songCrc = d.readInt()
                    songs.add(
                        Song(
                            code = code,
                            title = title,
                            artist = artist,
                            filePath = path,
                            packId = i,
                            packOffset = offset,
                            packLength = compLen,
                            packOrigLength = origLen,
                            packCrc = songCrc
                        )
                    )
                }
                packs[path] = PackInfo(file, nonce, key)
                Log.d(TAG, "Loaded ${songs.size} songs from ${file.name} in ${(System.nanoTime() - t0) / 1_000_000} ms")
                return songs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open pack ${file.absolutePath}", e)
            return emptyList()
        }
    }

    /** Decrypts a single song from its pack into memory. Returns the original MIDI bytes. */
    fun readSongBytes(song: Song): ByteArray? {
        if (song.packId < 0 || song.packOffset < 0L) return null
        val info = packs[song.filePath] ?: run {
            // Pack not opened yet in this process (e.g. song restored from state) – open it.
            loadSongs(File(song.filePath))
            packs[song.filePath]
        } ?: return null

        return try {
            val t0 = System.nanoTime()
            val enc = ByteArray(song.packLength)
            RandomAccessFile(info.file, "r").use { raf ->
                raf.seek(song.packOffset)
                raf.readFully(enc)
            }
            val raw = inflate(ctr(info.key, ivFor(info.nonce, song.packId.toLong()), enc), song.packOrigLength)
            if (crc(raw) != song.packCrc) {
                Log.e(TAG, "CRC mismatch for ${song.code} – pack corrupted?")
                return null
            }
            Log.d(TAG, "Decoded ${song.code} (${raw.size} bytes) in ${(System.nanoTime() - t0) / 1000} µs")
            raw
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read ${song.code} from pack", e)
            null
        }
    }

    /**
     * Writes the decrypted song to a private, randomly named temp file inside the app sandbox
     * (not visible to other apps or the file manager). Caller should delete it once the
     * player has opened it – see [SoundEngine.playMidi] `deleteAfterOpen`.
     */
    fun extractToPrivateTemp(song: Song, bytes: ByteArray, cacheDir: File): File? {
        return try {
            val dir = File(cacheDir, ".vkp").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // wipe leftovers from previous songs
            val f = File(dir, java.util.UUID.randomUUID().toString().replace("-", ""))
            f.writeBytes(bytes)
            f
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage ${song.code}", e)
            null
        }
    }
}
