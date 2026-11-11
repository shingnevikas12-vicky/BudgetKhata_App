package com.budgetkhata.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun `escapes commas quotes and newlines`() {
        assertEquals("\"Rahul, Patil\"", CsvExporter.escape("Rahul, Patil"))
        assertEquals("\"He said \"\"paid\"\"\"", CsvExporter.escape("He said \"paid\""))
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"))
        assertEquals("plain", CsvExporter.escape("plain"))
    }

    @Test
    fun `writes header and rows with carriage returns`() {
        val csv = CsvExporter.build(
            columns = listOf("Student", "Paid"),
            rows = listOf(listOf("Rahul", "\u20B9200"))
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("Student,Paid\r\n"))
        assertTrue(csv.contains("Rahul,\u20B9200\r\n"))
    }

    @Test
    fun `table export includes totals`() {
        val table = ReportTable(
            type = ReportType.PENDING,
            title = "Pending students",
            subtitle = "All events",
            columns = listOf(ReportColumn("Student", 1f), ReportColumn("Pending", 1f)),
            rows = listOf(listOf("Rahul", "\u20B9300")),
            totals = listOf("Pending" to "\u20B9300")
        )
        val csv = table.toCsv()
        assertTrue(csv.contains("Pending students"))
        assertTrue(csv.contains("Rahul"))
        assertTrue(csv.contains("Pending,\u20B9300"))
    }
}
