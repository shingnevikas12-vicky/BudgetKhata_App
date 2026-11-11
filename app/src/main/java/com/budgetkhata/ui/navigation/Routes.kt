package com.budgetkhata.ui.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val STUDENTS = "students?query={query}&status={status}"
    const val EVENTS = "events"
    const val EXPENSES = "expenses?eventId={eventId}"
    const val SETTINGS = "settings"
    const val REPORTS = "reports?eventId={eventId}"

    const val STUDENT_CREATE = "student_create"
    const val STUDENT_DETAIL = "student/{studentId}"
    const val STUDENT_EDIT = "student/{studentId}/edit"

    const val EVENT_CREATE = "event_create"
    const val EVENT_DETAIL = "event/{eventId}"
    const val EVENT_EDIT = "event/{eventId}/edit"

    const val EXPENSE_CREATE = "expense_create?eventId={eventId}"
    const val EXPENSE_EDIT = "expense/{expenseId}/edit"

    const val SETTINGS_UPI = "settings_upi"
    const val SETTINGS_REMINDER = "settings_reminder"
    const val SETTINGS_DATA = "settings_data"

    val topLevelBases = setOf("home", "students", "events", "expenses", "settings")

    fun students(query: String? = null, status: String? = null): String {
        val q = Uri.encode(query.orEmpty())
        val s = Uri.encode(status.orEmpty())
        return "students?query=$q&status=$s"
    }

    fun expenses(eventId: Long? = null): String = "expenses?eventId=${eventId ?: 0L}"

    fun reports(eventId: Long? = null): String = "reports?eventId=${eventId ?: 0L}"

    fun studentDetail(studentId: Long): String = "student/$studentId"

    fun studentEdit(studentId: Long): String = "student/$studentId/edit"

    fun eventDetail(eventId: Long): String = "event/$eventId"

    fun eventEdit(eventId: Long): String = "event/$eventId/edit"

    fun expenseCreate(eventId: Long? = null): String = "expense_create?eventId=${eventId ?: 0L}"

    fun expenseEdit(expenseId: Long): String = "expense/$expenseId/edit"
}
