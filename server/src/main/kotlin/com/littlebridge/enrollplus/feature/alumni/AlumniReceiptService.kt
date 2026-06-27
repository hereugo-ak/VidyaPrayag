/*
 * File: AlumniReceiptService.kt
 * Module: feature/alumni
 *
 * Purpose:
 *   80G-compliant donation receipt PDF generation + Form 10BD CSV export.
 *   Uses OpenPDF (com.github.librepdf:openpdf) for lightweight PDF generation.
 *
 * Spec ref: ALUMNI_MANAGEMENT_SPEC.md §B4, FR-10
 */
package com.littlebridge.enrollplus.feature.alumni

import com.littlebridge.enrollplus.db.*
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class AlumniReceiptService {

    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Generate an 80G-compliant receipt PDF for a single donation.
     * Returns the PDF bytes, or null if the donation doesn't exist or isn't 80G eligible.
     */
    suspend fun generateReceipt(schoolId: UUID, donationId: UUID): ByteArray? = withContext(Dispatchers.IO) {
        val data = dbQuery {
            val donation = AlumniDonationsTable.selectAll().where {
                (AlumniDonationsTable.id eq donationId) and (AlumniDonationsTable.schoolId eq schoolId)
            }.singleOrNull() ?: return@dbQuery null

            val alumniId = donation[AlumniDonationsTable.alumniId]
            val alumni = AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.singleOrNull()

            val school = SchoolsTable.selectAll().where { SchoolsTable.id eq schoolId }.singleOrNull()

            Triple(donation, alumni, school)
        } ?: return@withContext null

        val (donationRow, alumniRow, schoolRow) = data

        if (!donationRow[AlumniDonationsTable.is80gEligible]) return@withContext null

        val alumniName = alumniRow?.get(AlumniTable.name) ?: "Unknown"
        val alumniEmail = alumniRow?.get(AlumniTable.email)
        val alumniPhone = alumniRow?.get(AlumniTable.phone)
        val amount = donationRow[AlumniDonationsTable.amount]
        val donationDate = donationRow[AlumniDonationsTable.donationDate]
        val purpose = donationRow[AlumniDonationsTable.purpose] ?: "General"
        val paymentMode = donationRow[AlumniDonationsTable.paymentMode] ?: "N/A"
        val receiptNumber = donationRow[AlumniDonationsTable.receiptNumber] ?: "N/A"
        val referenceNumber = donationRow[AlumniDonationsTable.referenceNumber]

        val schoolName = schoolRow?.get(SchoolsTable.name) ?: "School"
        val panNumber = schoolRow?.get(SchoolsTable.panNumber)
        val g80RegNumber = schoolRow?.get(SchoolsTable.g80RegistrationNumber)

        val baos = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 54f, 36f)
        PdfWriter.getInstance(document, baos)
        document.open()

        // Header
        val headerFont = Font(Font.HELVETICA, 16f, Font.BOLD)
        val normalFont = Font(Font.HELVETICA, 11f, Font.NORMAL)
        val boldFont = Font(Font.HELVETICA, 11f, Font.BOLD)
        val smallFont = Font(Font.HELVETICA, 9f, Font.NORMAL)

        val title = Paragraph("$schoolName", headerFont)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)

        val receiptTitle = Paragraph("80G Donation Receipt", Font(Font.HELVETICA, 14f, Font.BOLD))
        receiptTitle.alignment = Element.ALIGN_CENTER
        receiptTitle.spacingAfter = 12f
        document.add(receiptTitle)

        // Receipt details table
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setSpacingBefore(4f)
        table.setSpacingAfter(4f)

        fun addRow(label: String, value: String) {
            table.addCell(PdfPCell(Paragraph(label, boldFont)).apply { borderWidth = 0.5f; paddingBottom = 6f; paddingTop = 6f })
            table.addCell(PdfPCell(Paragraph(value, normalFont)).apply { borderWidth = 0.5f; paddingBottom = 6f; paddingTop = 6f })
        }

        addRow("Receipt No.", receiptNumber)
        addRow("Date", donationDate.format(dateFmt))
        addRow("Donor Name", alumniName)
        if (alumniEmail != null) addRow("Donor Email", alumniEmail)
        if (alumniPhone != null) addRow("Donor Phone", alumniPhone)
        addRow("Amount", "Rs. ${"%,.2f".format(amount)}")
        addRow("Purpose", purpose)
        addRow("Payment Mode", paymentMode)
        if (referenceNumber != null) addRow("Reference No.", referenceNumber)

        document.add(table)
        document.add(Paragraph(" "))

        // 80G compliance section
        if (g80RegNumber != null) {
            val section80G = Paragraph("80G Registration No.: $g80RegNumber", boldFont)
            section80G.spacingBefore = 12f
            document.add(section80G)
        }
        if (panNumber != null) {
            document.add(Paragraph("PAN: $panNumber", normalFont))
        }

        val declaration = Paragraph(
            "This is to certify that the donation of Rs. ${"%,.2f".format(amount)} " +
                "has been received from $alumniName on ${donationDate.format(dateFmt)} " +
                "for the purpose of \"$purpose\". The donation is eligible for deduction " +
                "under Section 80G of the Income Tax Act, 1961.",
            smallFont
        )
        declaration.spacingBefore = 16f
        document.add(declaration)

        // Signature line
        document.add(Paragraph(" "))
        document.add(Paragraph(" "))
        val signLine = Paragraph("________________________", normalFont)
        signLine.alignment = Element.ALIGN_RIGHT
        document.add(signLine)
        val signLabel = Paragraph("Authorised Signatory", smallFont)
        signLabel.alignment = Element.ALIGN_RIGHT
        document.add(signLabel)

        // Footer
        document.add(Paragraph(" "))
        val footer = Paragraph(
            "This is a computer-generated receipt. Please retain for tax purposes.",
            Font(Font.HELVETICA, 8f, Font.ITALIC)
        )
        footer.alignment = Element.ALIGN_CENTER
        document.add(footer)

        document.close()
        baos.toByteArray()
    }

    /**
     * Export Form 10BD CSV — annual filing format for 80G donations.
     * Columns per Income Tax rules: Sr.No, Receipt No, Date, Donor Name, Donor PAN,
     * Donor Address, Amount, Mode, Purpose, 80G Reg No.
     */
    suspend fun exportForm10BD(schoolId: UUID, year: Int): ByteArray = withContext(Dispatchers.IO) {
        val startDate = LocalDate.of(year, 4, 1) // FY starts April 1
        val endDate = LocalDate.of(year + 1, 3, 31)

        val rows = dbQuery {
            val school = SchoolsTable.selectAll().where { SchoolsTable.id eq schoolId }.singleOrNull()
            val g80RegNumber = school?.get(SchoolsTable.g80RegistrationNumber) ?: ""
            val panNumber = school?.get(SchoolsTable.panNumber) ?: ""

            val donations = AlumniDonationsTable.selectAll().where {
                (AlumniDonationsTable.schoolId eq schoolId) and
                (AlumniDonationsTable.is80gEligible eq true) and
                (AlumniDonationsTable.donationDate greaterEq startDate) and
                (AlumniDonationsTable.donationDate lessEq endDate)
            }.orderBy(AlumniDonationsTable.donationDate, SortOrder.ASC).toList()

            donations.mapIndexed { idx, row ->
                val alumniId = row[AlumniDonationsTable.alumniId]
                val alumni = AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.singleOrNull()
                Form10BDRow(
                    srNo = idx + 1,
                    receiptNumber = row[AlumniDonationsTable.receiptNumber] ?: "",
                    date = row[AlumniDonationsTable.donationDate].format(dateFmt),
                    donorName = alumni?.get(AlumniTable.name) ?: "Unknown",
                    donorPAN = panNumber,
                    donorAddress = alumni?.get(AlumniTable.city) ?: "",
                    amount = row[AlumniDonationsTable.amount],
                    mode = row[AlumniDonationsTable.paymentMode] ?: "",
                    purpose = row[AlumniDonationsTable.purpose] ?: "General",
                    g80RegNumber = g80RegNumber
                )
            }
        }

        val sb = StringBuilder()
        sb.append("Sr.No,Receipt No.,Date,Donor Name,Donor PAN,Donor Address,Amount,Mode,Purpose,80G Reg No.\n")
        for (r in rows) {
            sb.append("${r.srNo},")
            sb.append("\"${r.receiptNumber}\",")
            sb.append("${r.date},")
            sb.append("\"${r.donorName}\",")
            sb.append("\"${r.donorPAN}\",")
            sb.append("\"${r.donorAddress}\",")
            sb.append("${"%.2f".format(r.amount)},")
            sb.append("${r.mode},")
            sb.append("\"${r.purpose}\",")
            sb.append("\"${r.g80RegNumber}\"\n")
        }

        sb.toString().toByteArray(Charsets.UTF_8)
    }

    private data class Form10BDRow(
        val srNo: Int,
        val receiptNumber: String,
        val date: String,
        val donorName: String,
        val donorPAN: String,
        val donorAddress: String,
        val amount: Double,
        val mode: String,
        val purpose: String,
        val g80RegNumber: String
    )
}
