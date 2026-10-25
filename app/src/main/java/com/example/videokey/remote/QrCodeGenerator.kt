package com.example.videokey.remote

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Standard QR Code & 2D Barcode Generator powered by ZXing.
 * Generates 100% compliant, scannable QR Code bitmaps that can be instantly
 * recognized by any phone camera, Google Lens, and QR scanner.
 */
object QrCodeGenerator {

    /**
     * Generates a scannable ISO/IEC 18004 QR Code Bitmap for [text].
     */
    fun generateQrBitmap(text: String, pixelSize: Int = 420): Bitmap? {
        if (text.isBlank()) return null
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, pixelSize, pixelSize, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, width, 0, 0, width, height)
            bmp
        } catch (e: Exception) {
            Log.e("QrCodeGenerator", "Failed to generate QR code for $text", e)
            null
        }
    }
}
