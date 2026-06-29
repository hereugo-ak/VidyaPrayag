package com.littlebridge.enrollplus.feature.idcard

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Renders ID card front and back as PNG images (54mm × 86mm at 300 DPI).
 *
 * Front: school name, logo, photo, name, role, class/department
 * Back: QR code, emergency contact, blood group, address, valid till
 *
 * Template config (OPT-07) drives field selection, colors, and font sizes.
 * Falls back to defaults when config is absent or invalid.
 */
object IdCardRenderer {

    private val json = Json { ignoreUnknownKeys = true }

    private const val CARD_W = 638   // 54mm at ~300 DPI
    private const val CARD_H = 1016  // 86mm at ~300 DPI

    @Serializable
    data class CardTemplateConfig(
        val fields: List<String> = listOf("name", "role", "class", "school"),
        val backgroundColor: String = "#FFFFFF",
        val textColor: String = "#000000",
        val accentColor: String? = null, // Override primaryColor from school branding
        val showPhoto: Boolean = true,
        val showLogo: Boolean = true,
        val showQrOnFront: Boolean = false,
        val titleFontSize: Int = 32,
        val bodyFontSize: Int = 22,
        val smallFontSize: Int = 18,
    ) {
        companion object {
            fun parse(jsonStr: String?): CardTemplateConfig {
                if (jsonStr.isNullOrBlank()) return CardTemplateConfig()
                return runCatching { json.decodeFromString<CardTemplateConfig>(jsonStr) }
                    .getOrElse { CardTemplateConfig() }
            }
        }
    }

    data class CardData(
        val personName: String,
        val personType: String,         // student | teacher | staff
        val classOrDept: String,        // "Class 7-B" or "Library"
        val schoolName: String,
        val schoolLogoUrl: String?,
        val photoUrl: String?,
        val qrCodePng: ByteArray,
        val emergencyContact: String?,
        val bloodGroup: String?,
        val address: String?,
        val validTill: String?,
        val primaryColor: String,       // hex like #2563EB
        val frontConfig: String? = null, // JSON CardTemplateConfig
        val backConfig: String? = null,  // JSON CardTemplateConfig
    )

    fun renderFront(data: CardData): ByteArray {
        val config = CardTemplateConfig.parse(data.frontConfig)
        val img = BufferedImage(CARD_W, CARD_H, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        val primary = Color.decode(config.accentColor ?: data.primaryColor)
        val bgColor = runCatching { Color.decode(config.backgroundColor) }.getOrDefault(Color.WHITE)
        val textColor = runCatching { Color.decode(config.textColor) }.getOrDefault(Color.BLACK)

        // Background
        g.color = bgColor
        g.fillRect(0, 0, CARD_W, CARD_H)

        // Header band
        g.color = primary
        g.fillRect(0, 0, CARD_W, 120)

        // School name (if in fields)
        if ("school" in config.fields) {
            g.color = Color.WHITE
            g.font = Font("SansSerif", Font.BOLD, 28)
            g.drawString(truncate(data.schoolName, 28), 20, 50)
        }

        // "ID CARD" label
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.PLAIN, 16)
        g.drawString("ID CARD", 20, 85)

        // Photo area (if showPhoto and in fields)
        val photoX = 20
        val photoY = 150
        val photoW = 200
        val photoH = 250
        if (config.showPhoto && "photo" in config.fields || (config.showPhoto && config.fields.contains("name"))) {
            if (data.photoUrl != null) {
                try {
                    val photoImg = loadRemoteImage(data.photoUrl, photoW, photoH)
                    g.drawImage(photoImg, photoX, photoY, photoW, photoH, null)
                } catch (_: Exception) {
                    drawPlaceholder(g, photoX, photoY, photoW, photoH, "PHOTO")
                }
            } else {
                drawPlaceholder(g, photoX, photoY, photoW, photoH, "PHOTO")
            }
        }

        // Name (if in fields)
        var textY = 200
        if ("name" in config.fields) {
            g.color = textColor
            g.font = Font("SansSerif", Font.BOLD, config.titleFontSize)
            g.drawString(truncate(data.personName, 24), 240, textY)
            textY += 40
        }

        // Role (if in fields)
        if ("role" in config.fields) {
            g.color = primary
            g.font = Font("SansSerif", Font.PLAIN, config.bodyFontSize)
            g.drawString(data.personType.replaceFirstChar { it.uppercase() }, 240, textY)
            textY += 30
        }

        // Class/Department (if in fields)
        if ("class" in config.fields) {
            g.color = Color.DARK_GRAY
            g.font = Font("SansSerif", Font.PLAIN, config.smallFontSize)
            g.drawString(truncate(data.classOrDept, 28), 240, textY)
            textY += 30
        }

        // QR on front (if showQrOnFront)
        if (config.showQrOnFront) {
            val qrSize = 120
            val qrX = CARD_W - qrSize - 20
            val qrY = CARD_H - qrSize - 80
            try {
                val qrImg = ImageIO.read(data.qrCodePng.inputStream())
                g.drawImage(qrImg, qrX, qrY, qrSize, qrSize, null)
            } catch (_: Exception) {
                drawPlaceholder(g, qrX, qrY, qrSize, qrSize, "QR")
            }
        }

        // Footer band
        g.color = primary
        g.fillRect(0, CARD_H - 60, CARD_W, 60)
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.PLAIN, 14)
        g.drawString("Vidya Prayag — Digital School Platform", 20, CARD_H - 25)

        g.dispose()
        return toPng(img)
    }

    fun renderBack(data: CardData): ByteArray {
        val config = CardTemplateConfig.parse(data.backConfig)
        val img = BufferedImage(CARD_W, CARD_H, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        val primary = Color.decode(config.accentColor ?: data.primaryColor)

        // Background
        g.color = runCatching { Color.decode(config.backgroundColor) }.getOrDefault(Color.WHITE)
        g.fillRect(0, 0, CARD_W, CARD_H)

        // Header band
        g.color = primary
        g.fillRect(0, 0, CARD_W, 60)

        // QR code
        val qrSize = 250
        val qrX = (CARD_W - qrSize) / 2
        val qrY = 100
        try {
            val qrImg = ImageIO.read(data.qrCodePng.inputStream())
            g.drawImage(qrImg, qrX, qrY, qrSize, qrSize, null)
        } catch (_: Exception) {
            drawPlaceholder(g, qrX, qrY, qrSize, qrSize, "QR")
        }

        // Fields (filtered by config)
        g.color = Color.BLACK
        g.font = Font("SansSerif", Font.PLAIN, config.smallFontSize)
        var y = 400
        if ("emergency_contact" in config.fields || config.fields.isEmpty()) {
            y = drawField(g, "Emergency Contact", data.emergencyContact ?: "N/A", y, config.smallFontSize)
        }
        if ("blood_group" in config.fields || config.fields.isEmpty()) {
            y = drawField(g, "Blood Group", data.bloodGroup ?: "N/A", y, config.smallFontSize)
        }
        if ("address" in config.fields || config.fields.isEmpty()) {
            y = drawField(g, "Address", truncate(data.address ?: "N/A", 40), y, config.smallFontSize)
        }
        if ("valid_till" in config.fields || config.fields.isEmpty()) {
            y = drawField(g, "Valid Till", data.validTill ?: "N/A", y, config.smallFontSize)
        }

        // Footer band
        g.color = primary
        g.fillRect(0, CARD_H - 60, CARD_W, 60)
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.PLAIN, 14)
        g.drawString("Scan QR to verify profile", 20, CARD_H - 25)

        g.dispose()
        return toPng(img)
    }

    private fun drawField(g: Graphics2D, label: String, value: String, y: Int, fontSize: Int): Int {
        g.color = Color.GRAY
        g.font = Font("SansSerif", Font.PLAIN, fontSize - 4)
        g.drawString(label, 40, y)
        g.color = Color.BLACK
        g.font = Font("SansSerif", Font.BOLD, fontSize)
        g.drawString(value, 40, y + 24)
        return y + 50
    }

    private fun drawPlaceholder(g: Graphics2D, x: Int, y: Int, w: Int, h: Int, text: String) {
        g.color = Color.LIGHT_GRAY
        g.fillRect(x, y, w, h)
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.PLAIN, 18)
        val fm = g.fontMetrics
        val tx = x + (w - fm.stringWidth(text)) / 2
        val ty = y + (h + fm.ascent - fm.descent) / 2
        g.drawString(text, tx, ty)
    }

    private fun truncate(s: String, max: Int): String =
        if (s.length <= max) s else s.substring(0, max - 1) + "…"

    private fun toPng(img: BufferedImage): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        return baos.toByteArray()
    }

    private fun loadRemoteImage(url: String, w: Int, h: Int): BufferedImage {
        val connection = java.net.URI(url).toURL().openConnection()
        connection.connectTimeout = 10_000  // 10 seconds to connect
        connection.readTimeout = 15_000     // 15 seconds to read
        val img = ImageIO.read(connection.getInputStream())
        val scaled = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.drawImage(img, 0, 0, w, h, null)
        g.dispose()
        return scaled
    }
}
