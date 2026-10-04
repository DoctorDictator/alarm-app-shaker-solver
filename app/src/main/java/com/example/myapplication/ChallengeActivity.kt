package com.example.myapplication

import android.content.Intent
import android.hardware.*
import android.os.*
import android.text.InputType
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.sqrt

class ChallengeActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensors: SensorManager
    private lateinit var progress: TextView
    private var alarm: Alarm? = null
    private var count = 0
    private lateinit var counter: ShakeCounter
    private var left = 0
    private var right = 0
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else { @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() {} })
        sensors = getSystemService(SensorManager::class.java)
        val id = intent.getIntExtra("id", -1)
        alarm = RingingService.activeAlarm?.takeIf { it.id == id }
        if (alarm == null || RingingService.activeId != id) { finish(); return }
        count = RingingService.shakeCount
        counter = ShakeCounter(count)
        left = RingingService.mathLeft
        right = RingingService.mathRight
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER; setPadding(32, 32, 32, 32) }
        setContentView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(layout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(32, bars.top + 32, 32, bars.bottom + 32); insets
        }
        layout.addView(TextView(this).apply { text = alarm!!.label; textSize = 30f })
        progress = TextView(this).apply { textSize = 24f }; layout.addView(progress)
        if (alarm!!.challenge == "Shake" && sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            updateCount()
            layout.addView(TextView(this).apply { text = "Shake back and forth at a comfortable pace. Hold your phone securely."; textSize = 17f })
        } else {
            progress.text = "$left × $right = ?"
            val answer = EditText(this).apply { hint = "Answer"; inputType = InputType.TYPE_CLASS_NUMBER; setSingleLine() }; layout.addView(answer)
            layout.addView(Button(this).apply { text = "Check answer"; setOnClickListener {
                if (answer.text.toString().toIntOrNull() == left * right) complete()
                else { answer.error = "Try again"; answer.text.clear() }
            } })
        }
    }
    override fun onResume() {
        super.onResume()
        if (alarm != null && RingingService.activeId != alarm!!.id) { finish(); return }
        if (alarm?.challenge == "Shake") sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
    }
    override fun onPause() { sensors.unregisterListener(this); super.onPause() }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("count", count); out.putInt("left", left); out.putInt("right", right); super.onSaveInstanceState(out) }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent) {
        val g = sqrt(event.values.take(3).sumOf { (it * it).toDouble() }) / SensorManager.GRAVITY_EARTH
        val now = SystemClock.elapsedRealtime()
        if (counter.sample(g, now)) {
            count = counter.count; RingingService.shakeCount = count; updateCount()
            if (count >= (alarm?.shakes ?: 20)) complete()
        }
    }
    private fun updateCount() { progress.text = "Shakes: $count / ${alarm?.shakes}" }
    private fun complete() {
        startService(Intent(this, RingingService::class.java).setAction("complete").putExtra("id", alarm?.id ?: -1))
        finish()
    }
}
