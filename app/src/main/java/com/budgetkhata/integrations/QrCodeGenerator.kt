package com.budgetkhata.integrations

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QrCodeGenerator {

    fun generate(content: String, sizePx: Int = 1024): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val dark = Color.BLACK
            val light = Color.WHITE
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) dark else light)
                }
            }
            bitmap
        } catch (error: Exception) {
            null
        }
    }

    fun saveToCache(context: Context, content: String, fileName: String): File? {
        val bitmap = generate(content) ?: return null
        val directory = File(context.cacheDir, "qr").apply { mkdirs() }
        val file = File(directory, fileName)
        return try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            file
        } catch (error: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }
}
