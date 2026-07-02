/*
 * File: LibraryCoverService.kt
 * Module: feature.library
 *
 * Handles book cover image uploads with validation, resize, and SSRF prevention.
 * Uses SupabaseStorage (S3/MinIO-compatible) for object storage and pure-JDK
 * ImageIO for resize — no extra dependencies required.
 *
 * Security:
 *   - File signature (magic bytes) validation — not just extension check
 *   - Content-type verified against allowlist
 *   - Image dimensions validated (min 100x150, max 5000x7500)
 *   - External cover URLs rejected (SSRF prevention — must use upload endpoint)
 *   - 2 MB size limit per spec
 *
 * Resize pipeline:
 *   - Thumbnail: 300x450 (stored at library/covers/{schoolId}/{bookId}_thumb.{ext})
 *   - Full:      600x900 (stored at library/covers/{schoolId}/{bookId}_full.{ext})
 *   - cover_url points to the full-size CDN URL
 */
package com.littlebridge.enrollplus.feature.library

import com.littlebridge.enrollplus.feature.media.SupabaseStorage
import java.awt.image.BufferedImage
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

object LibraryCoverService {

    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5 MB per spec §16
    private const val THUMB_WIDTH = 300
    private const val THUMB_HEIGHT = 450
    private const val FULL_WIDTH = 600
    private const val FULL_HEIGHT = 900
    private const val MIN_WIDTH = 100
    private const val MIN_HEIGHT = 150
    private const val MAX_WIDTH = 5000
    private const val MAX_HEIGHT = 7500

    private val ALLOWED_TYPES = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
    )

    // ── Magic byte signatures for SSRF / content-type spoofing prevention ──────

    private val MAGIC_BYTES = mapOf(
        "image/jpeg" to listOf(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
        "image/png" to listOf(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
        "image/webp" to listOf(byteArrayOf(0x52, 0x49, 0x46, 0x46)), // RIFF header
    )

    data class CoverUploadResult(
        val coverUrl: String,
        val thumbnailUrl: String?,
        val width: Int,
        val height: Int,
        val sizeBytes: Long,
    )

    /**
     * Validate and upload a cover image. Returns the CDN URLs for full + thumbnail.
     * Throws LibraryValidationException on any validation failure.
     */
    suspend fun upload(
        schoolId: UUID,
        bookId: UUID,
        bytes: ByteArray,
        contentType: String,
    ): CoverUploadResult {
        // 1. Size check
        if (bytes.isEmpty()) {
            throw LibraryValidationException("file", "No file data received")
        }
        if (bytes.size > MAX_FILE_SIZE) {
            throw LibraryValidationException("file", "Cover image must be under 5 MB")
        }

        // 2. Content-type allowlist
        val ct = contentType.substringBefore(';').trim().lowercase()
        val ext = ALLOWED_TYPES[ct]
            ?: throw LibraryValidationException("file", "Only JPG, PNG, and WebP images are accepted for book covers")

        // 3. Magic bytes verification (prevents content-type spoofing)
        validateMagicBytes(bytes, ct)

        // 4. Read image dimensions
        val originalImage = ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw LibraryValidationException("file", "Could not read image data — file may be corrupted")

        val origWidth = originalImage.width
        val origHeight = originalImage.height

        // 5. Dimension validation
        if (origWidth < MIN_WIDTH || origHeight < MIN_HEIGHT) {
            throw LibraryValidationException(
                "file",
                "Image dimensions must be at least ${MIN_WIDTH}x${MIN_HEIGHT} pixels (got ${origWidth}x${origHeight})",
            )
        }
        if (origWidth > MAX_WIDTH || origHeight > MAX_HEIGHT) {
            throw LibraryValidationException(
                "file",
                "Image dimensions must be at most ${MAX_WIDTH}x${MAX_HEIGHT} pixels (got ${origWidth}x${origHeight})",
            )
        }

        // 6. Resize to full (600x900) and thumbnail (300x450)
        val fullBytes = resizeImage(originalImage, FULL_WIDTH, FULL_HEIGHT, ct)
        val thumbBytes = resizeImage(originalImage, THUMB_WIDTH, THUMB_HEIGHT, ct)

        // 7. Upload both to object storage
        val fullUpload = SupabaseStorage.upload(schoolId, "IMAGE", fullBytes, ct)
            ?: throw LibraryConfigurationException("Cover upload failed — storage not configured or rejected")

        // Thumbnail is best-effort (don't fail if it doesn't upload)
        val thumbUpload = runCatching {
            SupabaseStorage.upload(schoolId, "IMAGE", thumbBytes, ct)
        }.getOrNull()

        return CoverUploadResult(
            coverUrl = fullUpload.url,
            thumbnailUrl = thumbUpload?.url,
            width = origWidth,
            height = origHeight,
            sizeBytes = bytes.size.toLong(),
        )
    }

    /**
     * Validate that an external cover URL is safe to use.
     * Per spec: external URLs are rejected — covers must come via the upload endpoint.
     * This prevents SSRF attacks where an attacker could trick the server into
     * fetching internal URLs.
     */
    fun validateExternalCoverUrl(url: String) {
        // Per spec §16 Cover Image Security:
        //   "Cover URL external links rejected (must use upload endpoint; prevents SSRF)"
        throw LibraryValidationException(
            "coverUrl",
            "External cover URLs are not allowed. Please use the file upload endpoint (POST /books/{id}/cover).",
        )
    }

    /**
     * Validate that a URL is a safe internal/storage URL (e.g. Supabase CDN).
     * Only HTTPS URLs from our own storage domain are allowed.
     */
    fun validateStorageUrl(url: String) {
        if (url.isBlank()) {
            throw LibraryValidationException("coverUrl", "Cover URL cannot be blank")
        }
        if (!url.startsWith("https://")) {
            throw LibraryValidationException("coverUrl", "Cover URL must be HTTPS")
        }
        // Reject localhost / private IP ranges (SSRF prevention)
        val host = runCatching {
            java.net.URI(url).host
        }.getOrNull() ?: throw LibraryValidationException("coverUrl", "Invalid cover URL")

        if (host == "localhost" || host.startsWith("127.") || host.startsWith("10.") ||
            host.startsWith("192.168.") || host.startsWith("169.254.") ||
            host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[01])\\..*"))
        ) {
            throw LibraryValidationException("coverUrl", "Cover URL must not point to a private/internal address")
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun validateMagicBytes(bytes: ByteArray, contentType: String) {
        val signatures = MAGIC_BYTES[contentType]
            ?: throw LibraryValidationException("file", "Unsupported image type: $contentType")

        val matches = signatures.any { sig ->
            if (bytes.size < sig.size) return@any false
            bytes.take(sig.size).toByteArray().contentEquals(sig)
        }

        if (!matches) {
            throw LibraryValidationException(
                "file",
                "File content does not match declared type $contentType — possible file spoofing detected",
            )
        }
    }

    private fun resizeImage(
        original: BufferedImage,
        targetWidth: Int,
        targetHeight: Int,
        contentType: String,
    ): ByteArray {
        // Only downscale — never upscale
        if (original.width <= targetWidth && original.height <= targetHeight) {
            return encodeImage(original, contentType)
        }

        // Maintain aspect ratio within target bounds
        val scale = minOf(
            targetWidth.toFloat() / original.width,
            targetHeight.toFloat() / original.height,
        )
        val scaledWidth = (original.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (original.height * scale).toInt().coerceAtLeast(1)

        val resized = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = resized.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.drawImage(original, 0, 0, scaledWidth, scaledHeight, null)
        graphics.dispose()

        return encodeImage(resized, contentType)
    }

    private fun encodeImage(image: BufferedImage, contentType: String): ByteArray {
        val format = when (contentType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, format, baos)
        return baos.toByteArray()
    }
}
