package com.yanni.nutritrack.data

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DayRangeTest {
    private fun localTime(zone: String, year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone(zone)).apply {
            clear()
            set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun usesLocalMidnightInsteadOfUtcMidnight() {
        val zone = "America/Sao_Paulo"
        val range = dayRange(localTime(zone, 2026, 9, 12, 12), TimeZone.getTimeZone(zone))
        assertEquals(localTime(zone, 2026, 9, 12, 0), range.start)
        assertEquals(localTime(zone, 2026, 9, 13, 0), range.end)
    }

    @Test
    fun daylightSavingDaysCanHave23Or25Hours() {
        val zone = "America/New_York"
        val spring = dayRange(localTime(zone, 2026, 3, 8, 12), TimeZone.getTimeZone(zone))
        val fall = dayRange(localTime(zone, 2026, 11, 1, 12), TimeZone.getTimeZone(zone))
        assertEquals(23 * 60 * 60 * 1000L, spring.end - spring.start)
        assertEquals(25 * 60 * 60 * 1000L, fall.end - fall.start)
    }

    @Test
    fun skippedMidnightDoesNotShiftTheFollowingDay() {
        val zone = "America/Sao_Paulo"
        val range = dayRange(localTime(zone, 2018, 11, 4, 12), TimeZone.getTimeZone(zone))
        assertEquals(localTime(zone, 2018, 11, 4, 1), range.start)
        assertEquals(localTime(zone, 2018, 11, 5, 0), range.end)
    }
}
