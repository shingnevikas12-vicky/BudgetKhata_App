package com.budgetkhata.domain

import com.budgetkhata.R

enum class ReportType(val labelRes: Int, val descRes: Int) {
    SUMMARY(R.string.report_summary_label, R.string.report_summary_desc),
    PAID(R.string.report_paid_label, R.string.report_paid_desc),
    PARTIAL(R.string.report_partial_label, R.string.report_partial_desc),
    PENDING(R.string.report_pending_label, R.string.report_pending_desc),
    LEDGER(R.string.report_ledger_label, R.string.report_ledger_desc),
    EXPENSES(R.string.report_expenses_label, R.string.report_expenses_desc)
}

data class ReportColumn(
    val header: String,
    val weight: Float
)

data class ReportTable(
    val type: ReportType,
    val title: String,
    val subtitle: String,
    val columns: List<ReportColumn>,
    val rows: List<List<String>>,
    val totals: List<Pair<String, String>> = emptyList(),
    val emptyMessage: String = "No records for this report"
) {
    fun toCsv(): String = CsvExporter.fromTable(this)
}
