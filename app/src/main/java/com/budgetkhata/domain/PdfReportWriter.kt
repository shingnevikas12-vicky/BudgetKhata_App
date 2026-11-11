package com.budgetkhata.domain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.budgetkhata.core.Dates
import java.io.File
import java.io.FileOutputStream

class PdfReportWriter(private val context: Context) {

    fun fileName(prefix: String, extension: String = "pdf"): String =
        "${prefix}-${Dates.fileStamp()}.$extension"

    fun write(tables: List<ReportTable>, documentTitle: String, fileName: String): File {
        val document = PdfDocument()
        val outputDirectory = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(outputDirectory, fileName)

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 22, 40)
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val metaPaint = Paint().apply {
            color = Color.rgb(95, 99, 120)
            textSize = 8.5f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(30, 27, 75)
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val cellPaint = Paint().apply {
            color = Color.rgb(30, 32, 48)
            textSize = 8.5f
            isAntiAlias = true
        }
        val boldCellPaint = Paint().apply {
            color = Color.rgb(20, 22, 40)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerBackground = Paint().apply { color = Color.rgb(67, 56, 202) }
        val alternateBackground = Paint().apply { color = Color.rgb(243, 244, 252) }
        val dividerPaint = Paint().apply {
            color = Color.rgb(214, 217, 230)
            strokeWidth = 0.5f
        }
        val footerPaint = Paint().apply {
            color = Color.rgb(120, 124, 145)
            textSize = 7.5f
            isAntiAlias = true
        }

        var pageNumber = 1
        var page: PdfDocument.Page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        var canvas: Canvas = page.canvas
        var y = MARGIN

        fun drawFooter() {
            val stamp = "Budget Khata \u00B7 $documentTitle \u00B7 page $pageNumber"
            canvas.drawText(stamp, MARGIN, PAGE_HEIGHT - 20f, footerPaint)
            val note = "Collections are manually confirmed by the administrator."
            val noteWidth = footerPaint.measureText(note)
            canvas.drawText(note, PAGE_WIDTH - MARGIN - noteWidth, PAGE_HEIGHT - 20f, footerPaint)
        }

        fun startNewPage() {
            drawFooter()
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN + 2f
        }

        fun ensureSpace(needed: Float): Boolean {
            if (y + needed > PAGE_HEIGHT - MARGIN - FOOTER_SPACE) {
                startNewPage()
                return true
            }
            return false
        }

        canvas.drawText(documentTitle, MARGIN, y + 14f, titlePaint)
        y += 26f
        canvas.drawText(
            "Generated ${Dates.formatMillis(System.currentTimeMillis())} \u00B7 All amounts in Indian Rupees",
            MARGIN,
            y,
            metaPaint
        )
        y += 22f

        tables.forEachIndexed { tableIndex, table ->
            if (tableIndex > 0) {
                ensureSpace(34f)
                y += 10f
            }

            canvas.drawText(table.title, MARGIN, y + 10f, sectionPaint)
            y += 17f
            canvas.drawText(table.subtitle, MARGIN, y, metaPaint)
            y += 14f

            val contentWidth = PAGE_WIDTH - 2 * MARGIN
            val totalWeight = table.columns.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.01f)
            val widths = table.columns.map { contentWidth * it.weight / totalWeight }

            fun drawHeaderRow() {
                val rowHeight = 18f
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBackground)
                var x = MARGIN + 4f
                table.columns.forEachIndexed { index, column ->
                    canvas.drawText(
                        elide(column.header, headerPaint, widths[index] - 8f),
                        x,
                        y + 12.5f,
                        headerPaint
                    )
                    x += widths[index]
                }
                y += rowHeight
            }

            drawHeaderRow()

            if (table.rows.isEmpty()) {
                ensureSpace(22f)
                canvas.drawText(table.emptyMessage, MARGIN + 4f, y + 14f, metaPaint)
                y += 22f
            } else {
                table.rows.forEachIndexed { rowIndex, row ->
                    val wrapped = ensureSpace(18f)
                    if (wrapped) drawHeaderRow()
                    val rowHeight = 16f
                    if (rowIndex % 2 == 1) {
                        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, alternateBackground)
                    }
                    var x = MARGIN + 4f
                    row.forEachIndexed { index, value ->
                        if (index < widths.size) {
                            canvas.drawText(
                                elide(value, cellPaint, widths[index] - 8f),
                                x,
                                y + 11.5f,
                                cellPaint
                            )
                            x += widths[index]
                        }
                    }
                    y += rowHeight
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
                }
            }

            if (table.totals.isNotEmpty()) {
                ensureSpace(14f + table.totals.size * 13f)
                y += 8f
                table.totals.forEach { (label, value) ->
                    canvas.drawText(label, MARGIN + 4f, y + 9f, boldCellPaint)
                    val valueWidth = boldCellPaint.measureText(value)
                    canvas.drawText(value, PAGE_WIDTH - MARGIN - 4f - valueWidth, y + 9f, boldCellPaint)
                    y += 13f
                }
            }
            y += 6f
        }

        drawFooter()
        document.finishPage(page)

        FileOutputStream(file).use { stream -> document.writeTo(stream) }
        document.close()
        return file
    }

    private fun elide(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (paint.measureText(text) <= maxWidth) return text
        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            val candidate = text.substring(0, mid) + "\u2026"
            if (paint.measureText(candidate) <= maxWidth) low = mid else high = mid - 1
        }
        return if (low <= 0) "" else text.substring(0, low) + "\u2026"
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 36f
        const val FOOTER_SPACE = 30f
    }
}
