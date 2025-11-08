package com.example.androidtimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private lateinit var ringtone: Ringtone
    private lateinit var vibrator: Vibrator

    companion object {
        const val ACTION_DISMISS = "com.example.androidtimer.ACTION_DISMISS"
        const val ALARM_STATE_UPDATE = "com.example.androidtimer.ALARM_STATE_UPDATE"
        const val IS_RINGING = "com.example.androidtimer.IS_RINGING"
    }

    override fun onCreate() {
        super.onCreate()
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, alarmSound)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun sendAlarmStateUpdate(isRinging: Boolean) {
        val intent = Intent(ALARM_STATE_UPDATE).apply {
            putExtra(IS_RINGING, isRinging)
            `package` = applicationContext.packageName
        }
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "AlarmChannel")
            .setContentTitle("Timer Expired!")
            .setContentText("Your timer has finished.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(activityPendingIntent)
            .setFullScreenIntent(activityPendingIntent, true)
            .setAutoCancel(true)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        startForeground(2, notification)
        ringtone.play()
        sendAlarmStateUpdate(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 500, 500, 500)
            val amplitudes = intArrayOf(0, 128, 0, 128)
            val vibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, 0)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 500, 500), 0)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ringtone.isPlaying) {
            ringtone.stop()
        }
        vibrator.cancel()
        sendAlarmStateUpdate(false)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "AlarmChannel",
                "Alarm Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
