package com.example.medicheckreminder.util

import android.content.Context
import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.DoseLog
import java.io.File

/**
 * Builds a shareable adherence PDF.
 *
 * Stub: the real implementation will use PDFBox (`com.tom_roush.pdfbox`) to write
 * a summary page plus the filtered daily dose logs into app cache storage.
 */
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

    @Suppress("UnusedParameter")
    fun export(context: Context, params: Params): Result {
        // TODO: PDDocument + PDPageContentStream rendering, then:
        // val file = File(context.cacheDir, "medicheck-adherence.pdf")
        return Result.Failure(R.string.export_pdf_not_ready)
    }
}
