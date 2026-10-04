package com.example.myapplication

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class NextAlarmTimeTest {
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private fun time(day: Int, hour: Int, minute: Int, second: Int = 0) = Calendar.getInstance(zone).apply {
        clear(); set(2026, Calendar.OCTOBER, day, hour, minute, second)
    }.timeInMillis

    @Test fun futureTimeIsToday() {
        assertEquals(time(4, 8, 30), nextAlarmTime(8, 30, time(4, 7, 0), zone))
    }
    @Test fun passedTimeIsTomorrow() {
        assertEquals(time(5, 7, 0), nextAlarmTime(7, 0, time(4, 7, 0, 1), zone))
    }
    @Test fun exactCurrentTimeDoesNotScheduleInThePast() {
        assertEquals(time(5, 7, 0), nextAlarmTime(7, 0, time(4, 7, 0), zone))
    }
    @Test fun midnightRollsToNextDay() {
        assertEquals(time(5, 0, 0), nextAlarmTime(0, 0, time(4, 23, 59), zone))
    }
    @Test fun passedDaylightSavingGapKeepsRequestedHourTomorrow() {
        val dstZone = TimeZone.getTimeZone("America/New_York")
        val now = Calendar.getInstance(dstZone).apply { clear(); set(2026, Calendar.MARCH, 8, 4, 0) }.timeInMillis
        val expected = Calendar.getInstance(dstZone).apply { clear(); set(2026, Calendar.MARCH, 9, 2, 30) }.timeInMillis
        assertEquals(expected, nextAlarmTime(2, 30, now, dstZone))
    }
}
