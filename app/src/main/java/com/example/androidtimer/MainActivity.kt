package com.example.androidtimer

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidtimer.ui.theme.AndroidTimerTheme
import kotlinx.coroutines.delay

// --- Data-driven Presets ---
data class PresetTimer(val displayText: String, val durationInSeconds: Long)

private val presetTimers = listOf(
    PresetTimer("4s", 4),
    PresetTimer("5m", 300),
    PresetTimer("10m", 600),
    PresetTimer("15m", 900),
    PresetTimer("21m", 1260),
    PresetTimer("30m", 1800),
    PresetTimer("45m", 2700),
    PresetTimer("1h", 3600),
    PresetTimer("90m", 5400)
)

// --- SharedPreferences Keys ---
private const val PREFS_NAME = "TimerPrefs"
private const val KEY_TRIGGER_TIME = "trigger_time"
private const val KEY_IS_RINGING = "is_ringing"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidTimerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { /* Handle permission result if needed */ }
        )
        SideEffect {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    
    var triggerTime by remember { mutableStateOf(prefs.getLong(KEY_TRIGGER_TIME, 0L)) }
    var remainingTime by remember { mutableStateOf(0L) }
    var isAlarmRinging by remember { mutableStateOf(prefs.getBoolean(KEY_IS_RINGING, false)) }

    var customHours by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf("") }
    var customSeconds by remember { mutableStateOf("") }

    LaunchedEffect(triggerTime) {
        while (triggerTime > System.currentTimeMillis()) {
            remainingTime = (triggerTime - System.currentTimeMillis()) / 1000
            delay(1000)
        }
        remainingTime = 0
    }
    
    DisposableEffect(Unit) {
        val alarmStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isAlarmRinging = intent?.getBooleanExtra(AlarmService.IS_RINGING, false) ?: false
                if (!isAlarmRinging) {
                    triggerTime = 0L
                    prefs.edit().remove(KEY_IS_RINGING).apply()
                }
            }
        }
        val alarmStateFilter = IntentFilter(AlarmService.ALARM_STATE_UPDATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(alarmStateReceiver, alarmStateFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(alarmStateReceiver, alarmStateFilter)
        }

        onDispose {
            context.unregisterReceiver(alarmStateReceiver)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isAlarmRinging -> "Time is up!"
                remainingTime > 0 -> "Time Remaining: ${formatTime(remainingTime)}"
                else -> "Timer is not running"
            },
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text("Presets")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(presetTimers) { preset ->
                Button(onClick = { triggerTime = startTimer(context, preset.durationInSeconds) }) {
                    Text(preset.displayText)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text("Custom")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
             OutlinedTextField(
                value = customHours,
                onValueChange = { customHours = it },
                label = { Text("H") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = customMinutes,
                onValueChange = { customMinutes = it },
                label = { Text("M") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = customSeconds,
                onValueChange = { customSeconds = it },
                label = { Text("S") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { 
                    val hours = customHours.toLongOrNull() ?: 0L
                    val minutes = customMinutes.toLongOrNull() ?: 0L
                    val seconds = customSeconds.toLongOrNull() ?: 0L
                    val totalSeconds = (hours * 3600) + (minutes * 60) + seconds
                    if (totalSeconds > 0) {
                        triggerTime = startTimer(context, totalSeconds)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start")
            }
            
            Button(
                onClick = {
                    if (isAlarmRinging) {
                        context.stopService(Intent(context, AlarmService::class.java))
                    } else if (remainingTime > 0) {
                        stopTimer(context)
                        triggerTime = 0L
                    }
                },
                enabled = isAlarmRinging || remainingTime > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%d:%02d:%02d", hours, minutes, secs)
}

private fun startTimer(context: Context, durationInSeconds: Long): Long {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TimerExpiredReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )

    val triggerTime = System.currentTimeMillis() + durationInSeconds * 1000
    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putLong(KEY_TRIGGER_TIME, triggerTime)
        .apply()

    return triggerTime
}

private fun stopTimer(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TimerExpiredReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .remove(KEY_TRIGGER_TIME)
        .apply()
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreview() {
    AndroidTimerTheme {
        TimerScreen()
    }
}
