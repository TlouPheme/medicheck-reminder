package com.example.medicheckreminder.util

import android.content.Context
import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportExporter {

    data class Params(
        val logs: List<DoseLog>,
        val rangeStartMillis: Long,
        val rangeEndMillis: Long,
        val title: String
    )

    sealed class Result {
        data class Success(val file: File) : Result()
        data class Failure(val messageRes: Int) : Result()
    }

    fun export(context: Context, params: Params): Result {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val file = File(context.cacheDir, "reports/medicheck-adherence.pdf")
            file.parentFile?.mkdirs()
            PDDocument().use { document ->
                var page = PDPage(PDRectangle.A4)
                document.addPage(page)
                var stream = PDPageContentStream(document, page)
                var y = page.mediaBox.height - MARGIN
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                stream.newLineAtOffset(MARGIN, y)
                stream.showText(ascii(params.title))
                stream.endText()
                y -= 28f

                val range = rangeLabel(params.rangeStartMillis, params.rangeEndMillis)
                y = drawLine(stream, y, range, bold = false)
                y -= 8f
                if (params.logs.isEmpty()) {
                    drawLine(stream, y, context.getString(R.string.empty_history_title), bold = false)
                }
                params.logs.forEach { log ->
                    if (y < MARGIN + 24f) {
                        stream.close()
                        page = PDPage(PDRectangle.A4)
                        document.addPage(page)
                        stream = PDPageContentStream(document, page)
                        y = page.mediaBox.height - MARGIN
                    }
                    val status = context.getString(statusRes(log.status))
                    val line = "${log.dateKey}  ${log.scheduledTime}  ${log.medicationName}  ${log.dosage}  $status"
                    y = drawLine(stream, y, line, bold = false)
                }
                stream.close()
                document.save(file)
            }
            Result.Success(file)
        } catch (_: Exception) {
            Result.Failure(R.string.export_pdf_failed)
        }
    }

    private fun drawLine(
        stream: PDPageContentStream,
        y: Float,
        text: String,
        bold: Boolean
    ): Float {
        stream.beginText()
        stream.setFont(if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA, 11f)
        stream.newLineAtOffset(MARGIN, y)
        stream.showText(ascii(text).take(110))
        stream.endText()
        return y - 16f
    }

    private fun rangeLabel(startMillis: Long, endMillis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "${format.format(Date(startMillis))} - ${format.format(Date(endMillis))}"
    }

    private fun statusRes(status: Dose.Status): Int = when (status) {
        Dose.Status.TAKEN -> R.string.status_taken
        Dose.Status.SKIPPED -> R.string.status_skipped
        Dose.Status.SNOOZED -> R.string.status_snoozed
        Dose.Status.PENDING -> R.string.status_pending
        Dose.Status.MISSED -> R.string.status_missed
    }

    private fun ascii(text: String): String {
        return text.map { char -> if (char.code in 32..126) char else ' ' }.joinToString("")
    }

    private companion object {
        const val MARGIN = 48f
    }
}
