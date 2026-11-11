package com.budgetkhata.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dates are stored as epoch-day longs (date only). Using Calendar keeps the app
 * free of java.time desugaring requirements on API 24.
 */
object Dates {

    private const val MILLIS_PER_DAY = 86_400_000L

    fun todayEpochDay(): Long = epochDayOf(System.currentTimeMillis())

    fun epochDayOf(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = millis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH)
            )
        }
        return utc.timeInMillis / MILLIS_PER_DAY
    }

    /** For values coming from Material3 DatePicker (UTC midnight millis). */
    fun fromPickerMillis(millis: Long): Long = millis / MILLIS_PER_DAY

    fun toDate(epochDay: Long): Date = Date(epochDay * MILLIS_PER_DAY)

    fun format(epochDay: Long?, pattern: String = "dd MMM yyyy"): String {
        if (epochDay == null) return "\u2014"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(toDate(epochDay))
    }

    fun formatMillis(millis: Long, pattern: String = "dd MMM yyyy, h:mm a"): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

    fun fileStamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())

    fun monthLabel(epochDay: Long): String = format(epochDay, "MMM yyyy")

    fun isSameMonth(epochDay: Long, referenceEpochDay: Long): Boolean =
        monthLabel(epochDay) == monthLabel(referenceEpochDay)
}
