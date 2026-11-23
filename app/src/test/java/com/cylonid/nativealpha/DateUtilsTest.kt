package com.cylonid.nativealpha
import com.cylonid.nativealpha.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateUtilsTest {
    @Test
    fun `getTimeInSeconds returns current time in seconds`() {
        val expected = System.currentTimeMillis() / 1000
        val actual = DateUtils.getTimeInSeconds()
        assertTrue(kotlin.math.abs(expected - actual) < 2)
    }

    @Test
    fun `getHourMinFormat returns correct format`() {
        val format = DateUtils.getHourMinFormat()
        val calendar =
            Calendar.getInstance().also {
                it.set(Calendar.HOUR_OF_DAY, 9)
                it.set(Calendar.MINUTE, 15)
            }
        assertEquals("09:15", format.format(calendar.time))
    }

    @Test
    fun `getDayHourMinuteSecondsFormat returns correct format`() {
        val format = DateUtils.getDayHourMinuteSecondsFormat()
        val legacyFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val parsedDate = legacyFormat.parse("08-04-2025 14:30")
        val formatted = format.format(parsedDate!!)
        assertTrue(formatted.contains("2025"))
    }

    @Test
    fun `convertStringToCalendar returns correct calendar object`() {
        val calendar = DateUtils.convertStringToCalendar("08:45")
        assertNotNull(calendar)
        assertEquals(8, calendar?.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, calendar?.get(Calendar.MINUTE))
    }

    @Test
    fun `convertStringToCalendar returns null on invalid input`() {
        val calendar = DateUtils.convertStringToCalendar("invalid")
        assertNull(calendar)
    }

    @Test
    fun `isInInterval returns true when time is within range`() {
        val format = DateUtils.getHourMinFormat()

        val low = Calendar.getInstance().also { it.time = format.parse("08:00")!! }
        val time = Calendar.getInstance().also { it.time = format.parse("09:00")!! }
        val high = Calendar.getInstance().also { it.time = format.parse("10:00")!! }

        assertTrue(DateUtils.isInInterval(low, time, high))
    }

    @Test
    fun `isInInterval returns false when time is outside range`() {
        val format = DateUtils.getHourMinFormat()

        val low = Calendar.getInstance().also { it.time = format.parse("08:00")!! }
        val time = Calendar.getInstance().also { it.time = format.parse("11:00")!! }
        val high = Calendar.getInstance().also { it.time = format.parse("10:00")!! }

        assertFalse(DateUtils.isInInterval(low, time, high))
    }

    @Test
    fun `isInInterval handles overnight intervals correctly`() {
        val format = DateUtils.getHourMinFormat()

        val low = Calendar.getInstance().also { it.time = format.parse("23:00")!! }
        val time = Calendar.getInstance().also { it.time = format.parse("01:00")!! }
        val high = Calendar.getInstance().also { it.time = format.parse("02:00")!! }

        assertTrue(DateUtils.isInInterval(low, time, high))
    }

    @Test
    fun `timestamp older than 14 days should return true`() {
        val fourteenDaysAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000 + 1)
        assertTrue(DateUtils.isOlderThanDays(fourteenDaysAgo, 14))
    }

    @Test
    fun `timestamp less than 14 days ago should return false`() {
        val thirteenDaysAgo = System.currentTimeMillis() - (13L * 24 * 60 * 60 * 1000)
        assertFalse(DateUtils.isOlderThanDays(thirteenDaysAgo, 14))
    }

    @Test
    fun `isInInterval does not mutate caller high Calendar`() {
        val format = DateUtils.getHourMinFormat()
        val low = Calendar.getInstance().also { it.time = format.parse("23:00")!! }
        val time = Calendar.getInstance().also { it.time = format.parse("01:00")!! }
        val high = Calendar.getInstance().also { it.time = format.parse("02:00")!! }
        val originalHighMillis = high.timeInMillis

        assertTrue(DateUtils.isInInterval(low, time, high))
        assertEquals(originalHighMillis, high.timeInMillis)
    }

    @Test
    fun `getHourMinFormat uses ASCII digits under non-Latin locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            val calendar =
                Calendar.getInstance().also {
                    it.set(Calendar.HOUR_OF_DAY, 9)
                    it.set(Calendar.MINUTE, 5)
                }
            assertEquals("09:05", DateUtils.getHourMinFormat().format(calendar.time))
        } finally {
            Locale.setDefault(original)
        }
    }
}
