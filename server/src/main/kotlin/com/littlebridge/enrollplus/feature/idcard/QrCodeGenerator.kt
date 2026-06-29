package com.littlebridge.enrollplus.feature.idcard

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Generates QR code PNG bytes from a string (typically a deep-link URL).
 */
object QrCodeGenerator {

    fun generatePng(data: String, width: Int = 300, height: Int = 300): ByteArray {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints)
        val image = MatrixToImageWriter.toBufferedImage(matrix)
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }
}
