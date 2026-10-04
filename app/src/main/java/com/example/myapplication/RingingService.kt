package com.example.myapplication

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat

class RingingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val waiting = ArrayDeque<Alarm>()
    companion object {
        var activeAlarm: Alarm? = null
            private set
        val activeId: Int? get() = activeAlarm?.id
        var shakeCount = 0
        var mathLeft = 0
        var mathRight = 0
        const val CHANNEL = "ringing_v1"
    }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "complete") {
            if (intent.getIntExtra("id", -1) != activeId) return START_NOT_STICKY
            releaseResources(); activeAlarm = null
            if (waiting.isEmpty()) { stopSelf(); return START_NOT_STICKY }
            ring(waiting.removeFirst())
            return START_NOT_STICKY
        }
        val id = intent?.getIntExtra("id", -1) ?: -1
        val alarm = AlarmStore(this).find(id) ?: return START_NOT_STICKY.also { if (activeId == null) stopSelf() }
        if (activeId != null && activeId != id) {
            if (waiting.none { it.id == id }) waiting.addLast(alarm)
            return START_NOT_STICKY
        }
        if (activeId != id) ring(alarm)
        return START_NOT_STICKY
    }
    private fun ring(alarm: Alarm) {
        releaseResources()
        activeAlarm = alarm
        shakeCount = 0
        mathLeft = kotlin.random.Random.nextInt(2, 10)
        mathRight = kotlin.random.Random.nextInt(2, 10)
        val id = alarm.id
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Open the alarm challenge"; setSound(null, null); enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            })
        val open = PendingIntent.getActivity(this, id,
            Intent(this, ChallengeActivity::class.java).putExtra("id", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(1, NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(alarm.label)
            .setContentText("Complete the ${alarm.challenge.lowercase()} challenge to stop")
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOngoing(true)
            .setContentIntent(open).setFullScreenIntent(open, true).build())
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:alarm").apply { acquire(60 * 60 * 1000L) }
        val defaultTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val tone = if (alarm.tone.isEmpty()) defaultTone else Uri.parse(alarm.tone)
        if (!play(tone) && !play(defaultTone)) play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        if (alarm.vibrate) {
            vibrator = getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            else { @Suppress("DEPRECATION") vibrator?.vibrate(longArrayOf(0, 500, 500), 0) }
        }
    }
    private fun play(uri: Uri?): Boolean {
        if (uri == null) return false
        val candidate = MediaPlayer()
        return try {
            candidate.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            candidate.setDataSource(this, uri); candidate.isLooping = true; candidate.prepare(); candidate.start()
            player = candidate; true
        } catch (_: Exception) { candidate.release(); false }
    }
    private fun releaseResources() {
        player?.release(); player = null; vibrator?.cancel(); vibrator = null
        wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
    }
    override fun onDestroy() { releaseResources(); activeAlarm = null; super.onDestroy() }
}
