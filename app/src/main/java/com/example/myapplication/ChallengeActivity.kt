package com.example.myapplication

import android.content.Intent
import android.hardware.*
import android.os.*
import android.text.InputType
import android.view.WindowManager
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChallengeActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensors: SensorManager
    private lateinit var progress: TextView
    private var alarm: Alarm? = null
    private var count = 0
    private lateinit var counter: ShakeCounter
    private var motionSensor: Sensor? = null
    private val gravityFilter = GravityFilter()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else { @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() {} })
        sensors = getSystemService(SensorManager::class.java)
        motionSensor = sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val id = intent.getIntExtra("id", -1)
        alarm = RingingService.activeAlarm?.takeIf { it.id == id }
        if (alarm == null || RingingService.activeId != id) { finish(); return }
        count = RingingService.shakeCount
        counter = ShakeCounter(count)
        val question = MathQuestion(RingingService.mathLeft, RingingService.mathRight)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER; setPadding(32, 32, 32, 32) }
        setContentView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(layout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(32, bars.top + 32, 32, bars.bottom + 32); insets
        }
        layout.addView(TextView(this).apply { text = alarm!!.label; textSize = 30f })
        progress = TextView(this).apply { textSize = 24f }; layout.addView(progress)
        if (alarm!!.challenge == "Shake" && motionSensor != null) {
            updateCount()
            layout.addView(TextView(this).apply { text = "Shake back and forth at a comfortable pace. Hold your phone securely."; textSize = 17f })
        } else {
            progress.text = question.prompt
            val answer = EditText(this).apply {
                hint = "Answer"; inputType = InputType.TYPE_CLASS_NUMBER; setSingleLine()
                imeOptions = EditorInfo.IME_ACTION_DONE
            }; layout.addView(answer)
            val submitAnswer = {
                if (question.accepts(answer.text.toString())) complete()
                else answer.error = "Try again"
            }
            layout.addView(Button(this).apply { text = "Check answer"; setOnClickListener { submitAnswer() } })
            answer.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)) {
                    submitAnswer(); true
                } else false
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (alarm != null && RingingService.activeId != alarm!!.id) { finish(); return }
        if (alarm?.challenge == "Shake" && motionSensor != null) {
            counter.resetMotion(); gravityFilter.reset()
            sensors.registerListener(this, motionSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }
    override fun onPause() { sensors.unregisterListener(this); super.onPause() }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent) {
        val now = event.timestamp / 1_000_000
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        val motion = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER)
            gravityFilter.sample(x, y, z, now) ?: return
        else doubleArrayOf(x, y, z)
        if (counter.sample(motion[0], motion[1], motion[2], now)) {
            count = counter.count; RingingService.shakeCount = count; updateCount()
            if (count >= (alarm?.shakes ?: 30)) complete()
        }
    }
    private fun updateCount() { progress.text = "Shakes: $count / ${alarm?.shakes}" }
    private fun complete() {
        startService(Intent(this, RingingService::class.java).setAction("complete").putExtra("id", alarm?.id ?: -1))
        finish()
    }
}
