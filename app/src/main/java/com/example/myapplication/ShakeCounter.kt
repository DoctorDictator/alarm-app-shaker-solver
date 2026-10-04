package com.example.myapplication

import kotlin.math.sqrt

/** Counts gentle changes of direction, rather than requiring a large impact. Values are m/s². */
class ShakeCounter(initialCount: Int = 0) {
    var count = initialCount
        private set
    private var direction: DoubleArray? = null
    private var lastTurn = 0L
    fun resetMotion() { direction = null }
    fun sample(x: Double, y: Double, z: Double, elapsedMillis: Long): Boolean {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return false
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude < 2.0) return false
        val current = doubleArrayOf(x / magnitude, y / magnitude, z / magnitude)
        val previous = direction
        if (previous == null || elapsedMillis - lastTurn > 1000) {
            direction = current; lastTurn = elapsedMillis; return false
        }
        // Opposing movement within one second counts once; repeated samples do not.
        val dot = previous.indices.sumOf { previous[it] * current[it] }
        if (dot > -0.45 || elapsedMillis - lastTurn < 120) return false
        direction = current
        lastTurn = elapsedMillis
        count++
        return true
    }
}

/** Fallback for phones that only expose the raw, gravity-inclusive accelerometer. */
class GravityFilter {
    private var gravity: DoubleArray? = null
    private var lastTime = 0L
    private var started = 0L
    fun reset() { gravity = null }
    fun sample(x: Double, y: Double, z: Double, time: Long): DoubleArray? {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        val values = doubleArrayOf(x, y, z)
        val estimate = gravity
        if (estimate == null || time - lastTime > 1000 || time <= lastTime) {
            gravity = values; lastTime = time; started = time; return null
        }
        val dt = (time - lastTime) / 1000.0
        val alpha = 0.8 / (0.8 + dt)
        lastTime = time
        val linear = DoubleArray(3) { axis ->
            estimate[axis] = alpha * estimate[axis] + (1 - alpha) * values[axis]
            values[axis] - estimate[axis]
        }
        return if (time - started < 300) null else linear
    }
}
