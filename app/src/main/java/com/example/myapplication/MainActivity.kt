package com.example.myapplication

import android.Manifest
import android.app.*
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var content: LinearLayout
    private var selectedTone = ""
    private var selectedToneName = "Phone default"
    private var toneButton: Button? = null
    private var hadExactAccess = false
    private var editorState: (() -> Bundle)? = null
    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedTone = uri.toString()
                selectedToneName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else "My audio"
                } ?: "My audio"
                toneButton?.text = selectedToneName
            } catch (_: Exception) { toast("Could not open this audio. Choose another file.") }
        }
    }
    private val pickSystem = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedTone = uri.toString(); selectedToneName = RingtoneManager.getRingtone(this, uri)?.getTitle(this) ?: "Phone alarm tone"
                toneButton?.text = selectedToneName
            }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        hadExactAccess = AlarmScheduler(this).allowed()
        AlarmScheduler(this).restore()
        render()
        state?.getBundle("editor")?.let { draft ->
            edit(AlarmStore(this).find(draft.getInt("id", -1)), draft)
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        editorState?.let { outState.putBundle("editor", it()) }
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        super.onResume()
        val allowed = AlarmScheduler(this).allowed()
        if (allowed && !hadExactAccess) AlarmScheduler(this).restore()
        hadExactAccess = allowed
        render()
    }
    private fun render() {
        content = column()
        setContentView(ScrollView(this).apply { addView(content) })
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(20), bars.top + dp(16), dp(20), bars.bottom + dp(16)); insets
        }
        content.addView(TextView(this).apply { text = "Simple Alarm"; textSize = 28f })
        content.addView(TextView(this).apply { text = "Wake up with a little effort."; textSize = 16f })
        if (!AlarmScheduler(this).allowed()) addButton("Allow alarms & reminders") {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            addButton("Allow alarm notifications") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10) }
        if (Build.VERSION.SDK_INT >= 34 && !getSystemService(NotificationManager::class.java).canUseFullScreenIntent())
            addButton("Allow alarm screen on lock screen") { startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))) }
        RingingService.activeId?.let { id -> addButton("Open ringing alarm") { startActivity(Intent(this, ChallengeActivity::class.java).putExtra("id", id)) } }
        addButton("Add alarm") { edit(null) }
        val alarms = AlarmStore(this).all()
        if (alarms.isEmpty()) content.addView(TextView(this).apply { text = "No alarms yet. Tap Add alarm to get started."; setPadding(0, dp(24), 0, 0) })
        alarms.forEach { alarm ->
            val row = column()
            row.addView(TextView(this).apply { text = String.format(Locale.getDefault(), "%02d:%02d  •  %s", alarm.hour, alarm.minute, alarm.label); textSize = 23f })
            row.addView(TextView(this).apply { text = "${if (alarm.daily) "Every day" else "Once"} · ${alarm.challenge}${if (alarm.challenge == "Shake") " (${alarm.shakes})" else ""}\n${alarm.toneName}" })
            row.addView(Switch(this).apply {
                text = "Enabled"; isChecked = alarm.enabled
                setOnCheckedChangeListener { _, checked ->
                    val changed = alarm.copy(enabled = checked)
                    if (AlarmScheduler(this@MainActivity).schedule(changed)) AlarmStore(this@MainActivity).save(changed)
                    else { toast("Allow alarms & reminders first."); isChecked = false }
                }
            })
            row.addView(Button(this).apply { text = "Edit"; setOnClickListener { edit(alarm) } })
            row.addView(Button(this).apply { text = "Delete"; setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setMessage("Delete this alarm?").setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ -> AlarmStore(this@MainActivity).delete(alarm.id); render() }.show()
            } })
            content.addView(row)
            content.addView(View(this).apply { setBackgroundColor(0xffcccccc.toInt()) }, LinearLayout.LayoutParams(-1, dp(1)))
        }
        content.addView(TextView(this).apply { text = "Uses your phone's alarm volume. Keep it above zero.\nOne-time alarms ring at the next selected time."; setPadding(0, dp(20), 0, 0) })
    }
    private fun edit(existing: Alarm?, draft: Bundle? = null) {
        selectedTone = draft?.getString("tone") ?: existing?.tone ?: ""
        selectedToneName = draft?.getString("toneName") ?: existing?.toneName ?: "Phone default"
        val form = column()
        val clock = TimePicker(this).apply { setIs24HourView(android.text.format.DateFormat.is24HourFormat(this@MainActivity)); hour = draft?.getInt("hour") ?: existing?.hour ?: 7; minute = draft?.getInt("minute") ?: existing?.minute ?: 0 }
        form.addView(clock)
        val label = EditText(this).apply { hint = "Alarm name"; setSingleLine(); setText(draft?.getString("label") ?: existing?.label ?: "Alarm") }; form.addView(label)
        form.addView(TextView(this).apply { text = "Stop alarm by completing:" })
        val mode = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Math", "Shake")); setSelection(draft?.getInt("mode") ?: if (existing?.challenge == "Shake") 1 else 0) }; form.addView(mode)
        val shakes = EditText(this).apply { hint = "Shake count (5–100)"; inputType = InputType.TYPE_CLASS_NUMBER; setText(draft?.getString("shakes") ?: (existing?.shakes ?: 20).toString()) }; form.addView(shakes)
        mode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { shakes.visibility = if (position == 1) View.VISIBLE else View.GONE }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        toneButton = Button(this).apply { text = selectedToneName; setOnClickListener { chooseTone() } }; form.addView(toneButton)
        val daily = CheckBox(this).apply { text = "Repeat every day"; isChecked = draft?.getBoolean("daily") ?: existing?.daily ?: false }; form.addView(daily)
        val vibration = CheckBox(this).apply { text = "Vibrate"; isChecked = draft?.getBoolean("vibrate") ?: existing?.vibrate ?: true }; form.addView(vibration)
        editorState = { Bundle().apply {
            putInt("id", existing?.id ?: -1); putInt("hour", clock.hour); putInt("minute", clock.minute)
            putString("label", label.text.toString()); putInt("mode", mode.selectedItemPosition)
            putString("shakes", shakes.text.toString()); putString("tone", selectedTone); putString("toneName", selectedToneName)
            putBoolean("daily", daily.isChecked); putBoolean("vibrate", vibration.isChecked)
        } }
        val dialog = AlertDialog.Builder(this).setTitle(if (existing == null) "New alarm" else "Edit alarm")
            .setView(ScrollView(this).apply { addView(form) }).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnDismissListener { editorState = null; toneButton = null }
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val count = shakes.text.toString().toIntOrNull()
            if (mode.selectedItemPosition == 1 && (count == null || count !in 5..100)) { shakes.error = "Enter 5 to 100"; return@setOnClickListener }
            if (mode.selectedItemPosition == 1 && getSystemService(android.hardware.SensorManager::class.java).getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) == null) { toast("This device has no shake sensor. Choose Math."); return@setOnClickListener }
            val alarm = Alarm(existing?.id ?: AlarmStore(this).nextId(), clock.hour, clock.minute,
                label.text.toString().trim().ifEmpty { "Alarm" }, mode.selectedItem.toString(), count ?: 20,
                selectedTone, selectedToneName, daily.isChecked, vibration.isChecked, existing?.enabled ?: true)
            if (!AlarmScheduler(this).schedule(alarm)) { toast("Allow alarms & reminders, then save again."); return@setOnClickListener }
            AlarmStore(this).save(alarm); dialog.dismiss(); toast("Alarm saved"); render()
        } }
        dialog.show()
    }
    private fun chooseTone() {
        AlertDialog.Builder(this).setTitle("Alarm sound").setItems(arrayOf("Phone default", "Phone alarm tones", "Choose audio file", "App ringtones")) { _, which ->
            when (which) {
                0 -> { selectedTone = ""; selectedToneName = "Phone default"; toneButton?.text = selectedToneName }
                1 -> try { pickSystem.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (selectedTone.isEmpty()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else Uri.parse(selectedTone))
                }) } catch (_: android.content.ActivityNotFoundException) { toast("No ringtone picker on this phone. Choose an audio file.") }
                2 -> pickAudio.launch(arrayOf("audio/*"))
                3 -> {
                    val tones = runCatching { Class.forName("$packageName.R\$raw").fields.filter { it.name.startsWith("alarm_") }.sortedBy { it.name }.take(10) }.getOrDefault(emptyList())
                    if (tones.isEmpty()) toast("No app tones added yet.")
                    else AlertDialog.Builder(this).setTitle("App ringtones").setItems(tones.map { it.name.removePrefix("alarm_").replace('_', ' ') }.toTypedArray()) { _, i ->
                        selectedTone = "android.resource://$packageName/raw/${tones[i].name}"
                        selectedToneName = tones[i].name.removePrefix("alarm_").replace('_', ' '); toneButton?.text = selectedToneName
                    }.show()
                }
            }
        }.show()
    }
    private fun addButton(title: String, action: () -> Unit) { content.addView(Button(this).apply { text = title; setOnClickListener { action() } }) }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
