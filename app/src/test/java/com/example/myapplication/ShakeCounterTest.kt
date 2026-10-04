package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class ShakeCounterTest {
    @Test fun invalidSensorReadingsDoNotCount() {
        val counter = ShakeCounter()
        assertFalse(counter.sample(Double.NaN, 0.0, 0.0, 1000))
        assertFalse(counter.sample(0.0, Double.POSITIVE_INFINITY, 0.0, 2000))
        assertEquals(0, counter.count)
    }
    @Test fun restingPhoneDoesNotCount() {
        val counter = ShakeCounter()
        repeat(100) { assertFalse(counter.sample(0.1, -0.1, 0.1, it * 20L)) }
        assertEquals(0, counter.count)
    }
    @Test fun sustainedAccelerationDoesNotCountAsShaking() {
        val counter = ShakeCounter()
        repeat(100) { assertFalse(counter.sample(3.0, 0.0, 0.0, it * 20L)) }
        assertEquals(0, counter.count)
    }
    @Test fun gentleReversalsCountWithoutHardImpacts() {
        val counter = ShakeCounter(4)
        assertFalse(counter.sample(2.5, 0.0, 0.0, 1000))
        assertTrue(counter.sample(-2.5, 0.0, 0.0, 1200))
        assertFalse(counter.sample(-2.6, 0.0, 0.0, 1250))
        assertTrue(counter.sample(2.5, 0.0, 0.0, 1400))
        assertEquals(6, counter.count)
    }
    @Test fun shortIntervalNoiseDoesNotCount() {
        val counter = ShakeCounter()
        counter.sample(2.5, 0.0, 0.0, 1000)
        assertFalse(counter.sample(-2.5, 0.0, 0.0, 1050))
        assertTrue(counter.sample(-2.5, 0.0, 0.0, 1200))
    }
    @Test fun slowUnrelatedMovementsDoNotCount() {
        val counter = ShakeCounter()
        counter.sample(2.5, 0.0, 0.0, 1000)
        assertFalse(counter.sample(-2.5, 0.0, 0.0, 2500))
        assertEquals(0, counter.count)
    }
    @Test fun resettingMotionPreservesProgressWithoutCountingOldDirection() {
        val counter = ShakeCounter(10)
        counter.sample(2.5, 0.0, 0.0, 1000)
        counter.resetMotion()
        assertFalse(counter.sample(-2.5, 0.0, 0.0, 1200))
        assertEquals(10, counter.count)
    }
    @Test fun shakingWorksAlongAnyAxis() {
        for (axis in 0..2) {
            val counter = ShakeCounter()
            val motion = DoubleArray(3).apply { this[axis] = 2.5 }
            counter.sample(motion[0], motion[1], motion[2], 1000)
            assertTrue(counter.sample(-motion[0], -motion[1], -motion[2], 1200))
        }
    }
    @Test fun smoothGentleMotionCountsRepeatedlyAtDifferentSensorRates() {
        for (interval in listOf(10L, 20L, 50L)) {
            val counter = ShakeCounter()
            var time = 0L
            while (time <= 10000) {
                val acceleration = 3.0 * kotlin.math.sin(2 * Math.PI * time / 500.0)
                counter.sample(acceleration, 0.0, 0.0, time)
                time += interval
            }
            assertTrue("At ${interval}ms, only ${counter.count} shakes counted", counter.count >= 30)
        }
    }
}
