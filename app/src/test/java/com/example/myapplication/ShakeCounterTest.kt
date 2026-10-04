package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class ShakeCounterTest {
    @Test fun invalidSensorReadingsDoNotCount() {
        val counter = ShakeCounter()
        assertFalse(counter.sample(Double.NaN, 1000))
        assertFalse(counter.sample(Double.POSITIVE_INFINITY, 2000))
        assertEquals(0, counter.count)
    }
    @Test fun restingPhoneDoesNotCount() {
        val counter = ShakeCounter()
        repeat(100) { assertFalse(counter.sample(1.0, it * 100L)) }
        assertEquals(0, counter.count)
    }
    @Test fun sustainedAccelerationOnlyCountsOnce() {
        val counter = ShakeCounter()
        assertTrue(counter.sample(2.8, 1000))
        assertFalse(counter.sample(3.0, 2000))
        assertEquals(1, counter.count)
    }
    @Test fun separateBurstsRespectCooldown() {
        val counter = ShakeCounter(4)
        assertTrue(counter.sample(2.8, 1000))
        counter.sample(1.0, 1100)
        assertFalse(counter.sample(2.8, 1200))
        assertTrue(counter.sample(2.8, 1400))
        assertEquals(6, counter.count)
    }
}
