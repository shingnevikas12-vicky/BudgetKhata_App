package com.budgetkhata.domain

import android.content.Context
import com.budgetkhata.R
import com.budgetkhata.core.Dates
import com.budgetkhata.core.Money
import com.budgetkhata.core.PaymentStatus
import com.budgetkhata.core.PhoneUtils
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.data.local.model.ExpenseRow

object ReportBuilder {

    fun build(
        context: Context,
        type: ReportType,
        rows: List<EnrollmentRow>,
        expenses: List<ExpenseRow>,
        summaries: List<EventSummary>,
        filterLabel: String
    ): ReportTable {
        val subtitle = "$filterLabel \u00B7 ${context.getString(R.string.done)} ${Dates.formatMillis(System.currentTimeMillis())}"
        return when (type) {
            ReportType.SUMMARY -> summaryTable(context, summaries, subtitle)
            ReportType.PAID -> studentTable(
                context = context,
                type = type,
                rows = rows.filter { it.status == PaymentStatus.PAID }.sortedBy { it.studentName.lowercase() },
                subtitle = subtitle,
                includePending = false
            )

            ReportType.PARTIAL -> studentTable(
                context = context,
                type = type,
                rows = rows.filter { it.status == PaymentStatus.PARTIAL }.sortedByDescending { it.remainingPaise },
                subtitle = subtitle,
                includePending = true
            )

            ReportType.PENDING -> studentTable(
                context = context,
                type = type,
                rows = rows.filter { it.status == PaymentStatus.PENDING }.sortedBy { it.studentName.lowercase() },
                subtitle = subtitle,
                includePending = true
            )

            ReportType.LEDGER -> studentTable(
                context = context,
                type = type,
                rows = rows.sortedWith(compareBy({ it.studentName.lowercase() }, { it.eventName.lowercase() })),
                subtitle = subtitle,
                includePending = true
            )

            ReportType.EXPENSES -> expenseTable(context, expenses, subtitle)
        }
    }

    private fun summaryTable(context: Context, summaries: List<EventSummary>, subtitle: String): ReportTable {
        val rows = summaries.map { summary ->
            listOf(
                summary.eventName,
                context.getString(summary.eventType.labelRes),
                summary.studentCount.toString(),
                Money.format(summary.expectedPaise),
                Money.format(summary.collectedPaise),
                Money.format(summary.pendingPaise),
                Money.format(summary.expensePaise),
                Money.format(summary.netPaise)
            )
        }
        val expected = summaries.sumOf { it.expectedPaise }
        val collected = summaries.sumOf { it.collectedPaise }
        val expenses = summaries.sumOf { it.expensePaise }
        return ReportTable(
            type = ReportType.SUMMARY,
            title = context.getString(R.string.financial_summary),
            subtitle = subtitle,
            columns = listOf(
                ReportColumn(context.getString(R.string.event), 2.2f),
                ReportColumn(context.getString(R.string.event_type), 1.4f),
                ReportColumn(context.getString(R.string.students), 0.9f),
                ReportColumn(context.getString(R.string.expected), 1.1f),
                ReportColumn(context.getString(R.string.collected), 1.1f),
                ReportColumn(context.getString(R.string.pending), 1.1f),
                ReportColumn(context.getString(R.string.expenses), 1.1f),
                ReportColumn(context.getString(R.string.net_balance), 1.1f)
            ),
            rows = rows,
            totals = listOf(
                context.getString(R.string.events) to summaries.size.toString(),
                context.getString(R.string.expected) to Money.format(expected),
                context.getString(R.string.collected) to Money.format(collected),
                context.getString(R.string.pending) to Money.format((expected - collected).coerceAtLeast(0L)),
                context.getString(R.string.total_expenses) to Money.format(expenses),
                context.getString(R.string.net_balance) to Money.format(collected - expenses)
            ),
            emptyMessage = context.getString(R.string.nothing_to_show)
        )
    }

    private fun studentTable(
        context: Context,
        type: ReportType,
        rows: List<EnrollmentRow>,
        subtitle: String,
        includePending: Boolean
    ): ReportTable {
        val columns = buildList {
            add(ReportColumn(context.getString(R.string.student), 1.9f))
            add(ReportColumn(context.getString(R.string.mobile_number), 1.5f))
            add(ReportColumn(context.getString(R.string.event), 1.9f))
            add(ReportColumn(context.getString(R.string.total_contribution), 1.2f))
            add(ReportColumn(context.getString(R.string.paid), 1.1f))
            if (includePending) add(ReportColumn(context.getString(R.string.pending), 1.1f))
            add(ReportColumn(context.getString(R.string.summary), 1.0f)) // Status
        }

        val tableRows = rows.map { row ->
            buildList {
                add(row.studentName)
                add(PhoneUtils.display(row.studentPhone))
                add(row.eventName)
                add(Money.format(row.requiredPaise))
                add(Money.format(row.paidPaise))
                if (includePending) add(Money.format(row.remainingPaise))
                add(context.getString(row.status.labelRes))
            }
        }

        val required = rows.sumOf { it.requiredPaise }
        val paid = rows.sumOf { it.paidPaise }

        return ReportTable(
            type = type,
            title = context.getString(type.labelRes),
            subtitle = subtitle,
            columns = columns,
            rows = tableRows,
            totals = listOf(
                context.getString(R.string.students) to rows.size.toString(),
                context.getString(R.string.total_contribution) to Money.format(required),
                context.getString(R.string.collected) to Money.format(paid),
                context.getString(R.string.pending) to Money.format((required - paid).coerceAtLeast(0L))
            ),
            emptyMessage = context.getString(R.string.nothing_to_show)
        )
    }

    private fun expenseTable(context: Context, expenses: List<ExpenseRow>, subtitle: String): ReportTable {
        val sorted = expenses.sortedByDescending { it.spentEpochDay }
        val rows = sorted.map { expense ->
            listOf(
                Dates.format(expense.spentEpochDay),
                expense.eventName,
                context.getString(expense.category.labelRes),
                expense.title,
                Money.format(expense.amountPaise),
                if (expense.receiptUri.isNullOrBlank()) "\u2014" else context.getString(R.string.ok) // Attached
            )
        }
        val total = sorted.sumOf { it.amountPaise }
        return ReportTable(
            type = ReportType.EXPENSES,
            title = context.getString(R.string.report_expenses_label),
            subtitle = subtitle,
            columns = listOf(
                ReportColumn(context.getString(R.string.payment_date), 1.3f),
                ReportColumn(context.getString(R.string.event), 1.9f),
                ReportColumn(context.getString(R.string.event_type), 1.8f), // Category
                ReportColumn(context.getString(R.string.description_optional), 2.4f),
                ReportColumn(context.getString(R.string.amount_paid), 1.1f),
                ReportColumn(context.getString(R.string.ok), 1.0f) // Receipt
            ),
            rows = rows,
            totals = listOf(
                context.getString(R.string.expenses) to sorted.size.toString(),
                context.getString(R.string.total_expenses) to Money.format(total)
            ),
            emptyMessage = context.getString(R.string.no_expenses_recorded)
        )
    }
}
