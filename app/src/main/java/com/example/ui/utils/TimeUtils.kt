package com.example.ui.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    /**
     * Formats timestamp into 12-hour format without spaces, e.g.: 9:26pm, 1:06pm, 5:00am
     */
    fun format12HourTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val amPm = if (hour24 < 12) "am" else "pm"
        val minuteStr = if (minute < 10) "0$minute" else "$minute"
        return "$hour12:$minuteStr$amPm"
    }

    /**
     * Formats last seen status with exact 12-hour time and spanish labels
     */
    fun formatLastSeen(lastSeen: Long, isCurrentlyActive: Boolean = false): String {
        if (isCurrentlyActive) return "en línea"
        if (lastSeen <= 0L) return "desconectado"

        val now = System.currentTimeMillis()
        val diff = now - lastSeen

        // If seen less than 2 minutes ago
        if (diff in 0..(2 * 60 * 1000L)) {
            return "en línea"
        }

        val targetCal = Calendar.getInstance().apply { timeInMillis = lastSeen }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val formattedTime = format12HourTime(lastSeen)

        val isSameDay = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return "últ. vez hoy a las $formattedTime"
        }

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return "últ. vez ayer a las $formattedTime"
        }

        val monthName = when (targetCal.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "ene."
            Calendar.FEBRUARY -> "feb."
            Calendar.MARCH -> "mar."
            Calendar.APRIL -> "abr."
            Calendar.MAY -> "may."
            Calendar.JUNE -> "jun."
            Calendar.JULY -> "jul."
            Calendar.AUGUST -> "ago."
            Calendar.SEPTEMBER -> "sep."
            Calendar.OCTOBER -> "oct."
            Calendar.NOVEMBER -> "nov."
            Calendar.DECEMBER -> "dic."
            else -> ""
        }
        val day = targetCal.get(Calendar.DAY_OF_MONTH)

        if (targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            return "últ. vez el $day de $monthName a las $formattedTime"
        }

        val year = targetCal.get(Calendar.YEAR)
        return "últ. vez el $day/$monthName/$year a las $formattedTime"
    }

    fun formatMessageTime(timestamp: Long): String {
        return format12HourTime(timestamp)
    }

    fun formatChatListDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = System.currentTimeMillis()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return format12HourTime(timestamp)
        }

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return "Ayer"
        }

        if (targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            val day = targetCal.get(Calendar.DAY_OF_MONTH)
            val month = SimpleDateFormat("MMM", Locale("es", "ES")).format(Date(timestamp))
            return "$day $month"
        }

        return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}
