package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = AlarmStore(context)
        val alarm = store.find(intent.getIntExtra("id", -1)) ?: return
        if (!alarm.enabled) return
        if (alarm.daily) AlarmScheduler(context).schedule(alarm)
        else store.save(alarm.copy(enabled = false))
        ContextCompat.startForegroundService(context, Intent(context, RingingService::class.java).putExtra("id", alarm.id))
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            )) return
        AlarmScheduler(context).restore()
    }
}
