package com.example.androidtimer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    companion object {
        const val TIMER_UPDATE = "com.example.androidtimer.TIMER_UPDATE"
        const val TIME_ELAPSED = "com.example.androidtimer.TIME_ELAPSED"
    }

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private var timer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun sendTimeUpdate(secondsRemaining: Long) {
        val timerUpdateIntent = Intent(TIMER_UPDATE).apply {
            putExtra(TIME_ELAPSED, secondsRemaining)
            // Make the broadcast explicit by setting the package name
            `package` = applicationContext.packageName
        }
        sendBroadcast(timerUpdateIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "TimerChannel")
            .setContentTitle("Timer is running")
            .setContentText("Timer is active in the background.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        val timerDuration = intent?.getLongExtra("TIMER_DURATION", 10L) ?: 10L

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, TimerExpiredReceiver::class.java)
        this.pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + timerDuration * 1000
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, this.pendingIntent)

        timer?.cancel()
        timer = object : CountDownTimer(timerDuration * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                sendTimeUpdate(millisUntilFinished / 1000)
            }

            override fun onFinish() {
                sendTimeUpdate(0L)
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        alarmManager.cancel(pendingIntent)
        // Send a final update to reset the UI
        sendTimeUpdate(0L)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "TimerChannel",
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
