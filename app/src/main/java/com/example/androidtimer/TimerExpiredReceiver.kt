package com.example.androidtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Stop the original TimerService, as its job is done.
        val timerServiceIntent = Intent(context, TimerService::class.java)
        context.stopService(timerServiceIntent)

        // Start the background AlarmService. It will handle the notification, sound,
        // vibration, and launching the full-screen UI.
        val alarmServiceIntent = Intent(context, AlarmService::class.java)
        context.startService(alarmServiceIntent)
    }
}
