package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class GravityFilterTest {
    @Test fun stationaryPhoneDoesNotCountInAnyOrientation() {
        for (axis in 0..2) {
            val filter = GravityFilter()
            val counter = ShakeCounter()
            val gravity = DoubleArray(3).apply { this[axis] = 9.81 }
            repeat(200) { index ->
                filter.sample(gravity[0], gravity[1], gravity[2], index * 20L)?.let {
                    assertFalse(counter.sample(it[0], it[1], it[2], index * 20L))
                }
            }
            assertEquals(0, counter.count)
        }
    }
    @Test fun rawAccelerometerGentleMotionCountsWithGravityRemoved() {
        val filter = GravityFilter()
        val counter = ShakeCounter()
        repeat(600) { index ->
            val time = index * 20L
            val acceleration = if (time < 500) 0.0 else 3.0 * kotlin.math.sin(2 * Math.PI * (time - 500) / 500.0)
            filter.sample(acceleration, 0.0, 9.81, time)?.let {
                counter.sample(it[0], it[1], it[2], time)
            }
        }
        assertTrue("Only ${counter.count} shakes counted", counter.count >= 30)
    }
    @Test fun startupAndResetDoNotCreateFalseShakes() {
        val filter = GravityFilter()
        assertNull(filter.sample(0.0, 0.0, 9.81, 1000))
        assertNull(filter.sample(0.0, 0.0, 9.81, 1100))
        filter.reset()
        assertNull(filter.sample(9.81, 0.0, 0.0, 2000))
    }
}
