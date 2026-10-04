package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

data class Alarm(
    val id: Int, val hour: Int, val minute: Int, val label: String = "Alarm",
    val challenge: String = "Math", val shakes: Int = 30, val tone: String = "",
    val toneName: String = "Phone default", val daily: Boolean = false,
    val vibrate: Boolean = true, val enabled: Boolean = true
)

class AlarmStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    fun all(): List<Alarm> {
        val array = JSONArray(prefs.getString("items", "[]"))
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Alarm(o.getInt("id"), o.getInt("hour"), o.getInt("minute"), o.getString("label"),
                o.getString("challenge"), o.getInt("shakes").coerceIn(30, 100), o.getString("tone"),
                o.getString("toneName"), o.getBoolean("daily"), o.getBoolean("vibrate"), o.getBoolean("enabled"))
        }.sortedWith(compareBy({ it.hour }, { it.minute }))
    }
    fun find(id: Int) = all().find { it.id == id }
    fun save(alarm: Alarm) = write(all().filterNot { it.id == alarm.id } + alarm)
    fun delete(id: Int) { AlarmScheduler(context).cancel(id); write(all().filterNot { it.id == id }) }
    private fun write(alarms: List<Alarm>) {
        val array = JSONArray()
        alarms.forEach { a -> array.put(JSONObject().apply {
            put("id", a.id); put("hour", a.hour); put("minute", a.minute); put("label", a.label)
            put("challenge", a.challenge); put("shakes", a.shakes); put("tone", a.tone)
            put("toneName", a.toneName); put("daily", a.daily); put("vibrate", a.vibrate); put("enabled", a.enabled)
        }) }
        prefs.edit().putString("items", array.toString()).commit()
    }
    fun nextId(): Int { val id = prefs.getInt("nextId", 1); prefs.edit().putInt("nextId", id + 1).commit(); return id }
}

class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    fun allowed() = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
    private fun pending(id: Int) = PendingIntent.getBroadcast(context, id,
        Intent(context, AlarmReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(alarm: Alarm): Boolean {
        if (!alarm.enabled) { cancel(alarm.id); return true }
        if (!allowed()) return false
        val time = nextAlarmTime(alarm.hour, alarm.minute)
        val show = PendingIntent.getActivity(context, alarm.id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return try { manager.setAlarmClock(AlarmManager.AlarmClockInfo(time, show), pending(alarm.id)); true }
        catch (_: SecurityException) { false }
    }
    fun cancel(id: Int) = manager.cancel(pending(id))
    fun restore() { AlarmStore(context).all().filter { it.enabled }.forEach { schedule(it) } }
}
