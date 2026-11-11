package com.budgetkhata.domain

object CsvExporter {

    fun fromTable(table: ReportTable): String {
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.append(escape(table.title)).append("\r\n")
        builder.append(escape(table.subtitle)).append("\r\n\r\n")
        builder.append(table.columns.joinToString(",") { escape(it.header) }).append("\r\n")
        table.rows.forEach { row ->
            builder.append(row.joinToString(",") { escape(it) }).append("\r\n")
        }
        if (table.totals.isNotEmpty()) {
            builder.append("\r\n")
            table.totals.forEach { (label, value) ->
                builder.append(escape(label)).append(",").append(escape(value)).append("\r\n")
            }
        }
        return builder.toString()
    }

    fun build(columns: List<String>, rows: List<List<String>>): String {
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.append(columns.joinToString(",") { escape(it) }).append("\r\n")
        rows.forEach { row ->
            builder.append(row.joinToString(",") { escape(it) }).append("\r\n")
        }
        return builder.toString()
    }

    fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
