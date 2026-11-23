package com.cylonid.nativealpha.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Objects.requireNonNull

object DateUtils {
    @JvmStatic
    fun getTimeInSeconds(): Long = System.currentTimeMillis() / 1000

    @JvmStatic
    fun getHourMinFormat(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.US)

    @JvmStatic
    fun getDayHourMinuteSecondsFormat(): SimpleDateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)

    @JvmStatic
    fun convertStringToCalendar(str: String?): Calendar? {
        if (str.isNullOrBlank()) return null
        return try {
            val parsedDate = getHourMinFormat().parse(str)
            Calendar.getInstance().also { it.time = parsedDate!! }
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun isInInterval(
        low: Calendar,
        time: Calendar,
        high: Calendar,
    ): Boolean {
        // Bring timestamp with day_current + HH:mm => day_unixZero + HH:mm by parsing it again...
        val middle = Calendar.getInstance()
        middle.time =
            requireNonNull(
                getHourMinFormat().parse(
                    getHourMinFormat().format(time.time),
                ),
            )

        // CASE: If the end of our timespan is after midnight, add one day to the end date to get a proper span.
        val adjustedHigh = high.clone() as Calendar
        if (adjustedHigh.before(low)) {
            adjustedHigh.add(Calendar.DATE, 1)
            if (middle.before(low)) {
                middle.add(Calendar.DATE, 1)
            }
        }
        return middle.after(low) && middle.before(adjustedHigh)
    }

    @JvmStatic
    fun isOlderThanDays(
        timestamp: Long,
        days: Int,
        targetTime: Long = System.currentTimeMillis(),
    ): Boolean {
        val daysInMillis = days * 24L * 60 * 60 * 1000
        return (targetTime - timestamp) > daysInMillis
    }
}
