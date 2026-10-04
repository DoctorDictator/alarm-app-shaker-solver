package com.example.myapplication

import java.util.Calendar
import java.util.TimeZone

fun nextAlarmTime(hour: Int, minute: Int, now: Long = System.currentTimeMillis(),
                  zone: TimeZone = TimeZone.getDefault()): Long {
    require(hour in 0..23 && minute in 0..59)
    return Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) {
            // Restore the requested hour after a daylight-saving gap normalized today's time.
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
    }.timeInMillis
}
