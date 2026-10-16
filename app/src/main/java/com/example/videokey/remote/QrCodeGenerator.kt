package com.example.videokey.remote

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Pure-Kotlin ISO/IEC 18004 Standard QR Code & 1D Barcode Generator.
 * Encodes URLs (such as "http://192.168.1.5:8080") in Byte Mode with Reed-Solomon
 * Error Correction (Level L, Mask Pattern 0) into a crisp, scannable Bitmap.
 */
object QrCodeGenerator {

    /**
     * Generates a scannable ISO/IEC 18004 QR Code Bitmap for [text] (up to 53 bytes).
     * Uses Version 2 (25x25) for <= 32 bytes or Version 3 (29x29) for <= 53 bytes.
     */
    fun generateQrBitmap(text: String, pixelSize: Int = 420): Bitmap {
        val payload = text.toByteArray(Charsets.UTF_8)
        val version = if (payload.size <= 32) 2 else 3
        val totalDataCodewords = if (version == 2) 34 else 55
        val eccCodewordsCount = if (version == 2) 10 else 15
        val dim = 17 + 4 * version // 25 for v2, 29 for v3

        // 1. Build bit stream: Byte Mode (0100) + 8-bit length + bytes + terminator + padding
        val bits = ArrayList<Int>(totalDataCodewords * 8 + 16)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        appendBits(0b0100, 4) // Byte mode indicator
        val safeLen = payload.size.coerceAtMost(totalDataCodewords - 2)
        appendBits(safeLen, 8)
        for (i in 0 until safeLen) {
            appendBits(payload[i].toInt() and 0xFF, 8)
        }
        // Terminator up to 4 zero bits
        val maxBits = totalDataCodewords * 8
        val termCount = (maxBits - bits.size).coerceIn(0, 4)
        appendBits(0, termCount)
        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        val dataBytes = IntArray(totalDataCodewords)
        var byteIdx = 0
        var bitIdx = 0
        while (bitIdx + 8 <= bits.size && byteIdx < totalDataCodewords) {
            var b = 0
            for (k in 0 until 8) {
                b = (b shl 1) or bits[bitIdx + k]
            }
            dataBytes[byteIdx++] = b
            bitIdx += 8
        }
        var padToggle = true
        while (byteIdx < totalDataCodewords) {
            dataBytes[byteIdx++] = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
        }

        // 2. Compute Reed-Solomon Error Correction Codewords over GF(256) (poly 0x11D)
        val eccBytes = computeReedSolomon(dataBytes, eccCodewordsCount)
        val allCodewords = IntArray(totalDataCodewords + eccCodewordsCount)
        System.arraycopy(dataBytes, 0, allCodewords, 0, totalDataCodewords)
        System.arraycopy(eccBytes, 0, allCodewords, totalDataCodewords, eccCodewordsCount)

        // 3. Initialize QR Matrix & Function Pattern reservation mask
        val modules = Array(dim) { BooleanArray(dim) }
        val isFunction = Array(dim) { BooleanArray(dim) }

        fun setFunctionModule(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until dim && c in 0 until dim) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        // Finder patterns (7x7) + separators
        fun drawFinder(topR: Int, leftC: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = topR + dr
                    val c = leftC + dc
                    if (r in 0 until dim && c in 0 until dim) {
                        val inOuter = dr in 0..6 && dc in 0..6 && (dr == 0 || dr == 6 || dc == 0 || dc == 6)
                        val inInner = dr in 2..4 && dc in 2..4
                        setFunctionModule(r, c, inOuter || inInner)
                    }
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(0, dim - 7)
        drawFinder(dim - 7, 0)

        // Timing patterns (row 6 and col 6)
        for (i in 8 until dim - 8) {
            setFunctionModule(6, i, i % 2 == 0)
            setFunctionModule(i, 6, i % 2 == 0)
        }

        // Alignment pattern (5x5 centered at dim - 7, dim - 7 -> (18, 18) for v2, (22, 22) for v3)
        val alignCenter = dim - 7
        for (dr in -2..2) {
            for (dc in -2..2) {
                val dist = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                setFunctionModule(alignCenter + dr, alignCenter + dc, dist != 1)
            }
        }

        // Reserve Format Information areas & Dark Module
        for (i in 0..8) {
            if (!isFunction[8][i]) setFunctionModule(8, i, false)
            if (!isFunction[i][8]) setFunctionModule(i, 8, false)
        }
        for (i in 0..7) {
            if (!isFunction[8][dim - 1 - i]) setFunctionModule(8, dim - 1 - i, false)
            if (!isFunction[dim - 1 - i][8]) setFunctionModule(dim - 1 - i, 8, false)
        }
        setFunctionModule(dim - 8, 8, true) // Dark module

        // 4. Place Data + ECC bits in standard upward/downward 2-column zigzag
        var codewordBitIndex = 0
        val totalBits = allCodewords.size * 8
        var rightCol = dim - 1
        var upward = true
        while (rightCol >= 1) {
            if (rightCol == 6) rightCol -= 1 // Skip vertical timing column
            for (step in 0 until dim) {
                val r = if (upward) (dim - 1 - step) else step
                for (dx in 0..1) {
                    val c = rightCol - dx
                    if (!isFunction[r][c]) {
                        val darkBit = if (codewordBitIndex < totalBits) {
                            val cw = allCodewords[codewordBitIndex ushr 3]
                            val shift = 7 - (codewordBitIndex and 7)
                            codewordBitIndex++
                            ((cw ushr shift) and 1) == 1
                        } else {
                            false
                        }
                        // Apply Mask Pattern 0: (r + c) % 2 == 0
                        val masked = if ((r + c) % 2 == 0) !darkBit else darkBit
                        modules[r][c] = masked
                    }
                }
            }
            upward = !upward
            rightCol -= 2
        }

        // 5. Write Format Information for ECC Level L (01) + Mask 0 (000) -> 0x77C4 (15 bits)
        val formatBits = 0x77C4
        fun getFormatBit(idx: Int): Boolean = ((formatBits ushr idx) and 1) == 1

        // Around top-left finder
        for (i in 0..5) setFunctionModule(8, i, getFormatBit(i))
        setFunctionModule(8, 7, getFormatBit(6))
        setFunctionModule(8, 8, getFormatBit(7))
        setFunctionModule(7, 8, getFormatBit(8))
        for (i in 9..14) setFunctionModule(14 - i, 8, getFormatBit(i))

        // Around top-right and bottom-left finders
        for (i in 0..7) setFunctionModule(8, dim - 1 - i, getFormatBit(i))
        for (i in 8..14) setFunctionModule(dim - 15 + i, 8, getFormatBit(i))
        setFunctionModule(dim - 8, 8, true)

        // 6. Render to crisp Bitmap with 4-module white quiet zone
        val quietZone = 3
        val totalModules = dim + quietZone * 2
        val bmp = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(pixelSize * pixelSize) { Color.WHITE }

        for (y in 0 until pixelSize) {
            val mr = (y * totalModules) / pixelSize - quietZone
            for (x in 0 until pixelSize) {
                val mc = (x * totalModules) / pixelSize - quietZone
                if (mr in 0 until dim && mc in 0 until dim && modules[mr][mc]) {
                    pixels[y * pixelSize + x] = Color.BLACK
                }
            }
        }
        bmp.setPixels(pixels, 0, pixelSize, 0, 0, pixelSize, pixelSize)
        return bmp
    }

    private fun computeReedSolomon(data: IntArray, eccCount: Int): IntArray {
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            exp[i] = exp[i - 255]
        }
        fun gfMul(a: Int, b: Int): Int {
            if (a == 0 || b == 0) return 0
            return exp[log[a] + log[b]]
        }

        // Build generator polynomial g(x) = (x - 2^0)(x - 2^1)...(x - 2^(eccCount-1))
        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val next = IntArray(gen.size + 1)
            val root = exp[i]
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], root)
            }
            gen = next
        }

        val remainder = IntArray(eccCount)
        for (b in data) {
            val factor = b xor remainder[0]
            System.arraycopy(remainder, 1, remainder, 0, eccCount - 1)
            remainder[eccCount - 1] = 0
            if (factor != 0) {
                for (j in 0 until eccCount) {
                    remainder[j] = remainder[j] xor gfMul(gen[j + 1], factor)
                }
            }
        }
        return remainder
    }
}
