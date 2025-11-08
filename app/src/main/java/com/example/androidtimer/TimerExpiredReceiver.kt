package com.example.androidtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Constants moved here for receiver access
private const val PREFS_NAME = "TimerPrefs"
private const val KEY_TRIGGER_TIME = "trigger_time"

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Clear the persisted trigger time
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_TRIGGER_TIME)
            .apply()

        // Start the background AlarmService for reliable sound and vibration.
        val alarmServiceIntent = Intent(context, AlarmService::class.java)
        context.startService(alarmServiceIntent)
    }
}
