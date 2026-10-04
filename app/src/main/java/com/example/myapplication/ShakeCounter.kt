package com.example.myapplication

/** Counts separated bursts, so a sustained acceleration cannot finish the alarm. */
class ShakeCounter(initialCount: Int = 0) {
    var count = initialCount
        private set
    private var ready = true
    private var lastShake = -350L
    fun sample(gravity: Double, elapsedMillis: Long): Boolean {
        if (!gravity.isFinite()) return false
        if (gravity < 1.3) ready = true
        if (gravity <= 2.2 || !ready || elapsedMillis - lastShake < 350) return false
        ready = false
        lastShake = elapsedMillis
        count++
        return true
    }
}
