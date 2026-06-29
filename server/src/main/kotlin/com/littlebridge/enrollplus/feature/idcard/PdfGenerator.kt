package com.littlebridge.enrollplus.feature.idcard

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/**
 * Generates a printable PDF with ID card images (2 per A4 page: front + back).
 *
 * Each "card pair" is the front image on the left half and the back image on
 * the right half of an A4 page, sized to ID-card proportions (54mm × 86mm).
 *
 * For large batches (100+ cards), use [generateToTempFile] to avoid holding
 * the entire PDF in memory. Use [generate] for small batches only.
 */
object PdfGenerator {

    private const val TEMP_PREFIX = "idcard-pdf-"
    private const val TEMP_SUFFIX = ".pdf"
    private const val MEMORY_THRESHOLD = 50 // Use temp file for 50+ card pairs

    /**
     * Generates PDF in memory. Suitable for small batches (< 50 cards).
     *
     * @param cardPairs list of (frontPng, backPng) byte arrays
     * @return PDF bytes
     */
    fun generate(cardPairs: List<Pair<ByteArray, ByteArray>>): ByteArray {
        if (cardPairs.size > MEMORY_THRESHOLD) {
            // Use temp file for large batches, then read back
            val tempFile = generateToTempFile(cardPairs)
            return Files.readAllBytes(tempFile.toPath()).also { tempFile.delete() }
        }

        val baos = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, baos)
        document.open()
        writePages(document, cardPairs)
        document.close()
        return baos.toByteArray()
    }

    /**
     * Streams PDF to a temp file on disk. Suitable for large batches (50+ cards).
     * Caller is responsible for deleting the temp file after upload.
     *
     * @param cardPairs list of (frontPng, backPng) byte arrays
     * @return temp file containing the PDF
     */
    fun generateToTempFile(cardPairs: List<Pair<ByteArray, ByteArray>>): File {
        val tempFile = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX)
        tempFile.deleteOnExit() // Safety net in case caller forgets to delete

        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, tempFile.outputStream())
        document.open()
        writePages(document, cardPairs)
        document.close()
        return tempFile
    }

    private fun writePages(
        document: Document,
        cardPairs: List<Pair<ByteArray, ByteArray>>,
    ) {
        val pageWidth = PageSize.A4.width - 72f  // minus margins
        val cardWidth = pageWidth / 2f - 10f
        val cardHeight = cardWidth * (86f / 54f)

        for ((front, back) in cardPairs) {
            val frontImg = Image.getInstance(front)
            frontImg.scaleAbsolute(cardWidth, cardHeight)
            frontImg.alignment = Element.ALIGN_LEFT
            document.add(frontImg)

            val backImg = Image.getInstance(back)
            backImg.scaleAbsolute(cardWidth, cardHeight)
            backImg.alignment = Element.ALIGN_RIGHT
            backImg.spacingBefore = -cardHeight
            document.add(backImg)

            document.newPage()
        }
    }
}
