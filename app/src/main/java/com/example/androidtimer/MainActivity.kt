package com.example.androidtimer

import android.Manifest
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
            onResult = { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted
                } else {
                    // Permission denied
                }
            }
        )
        SideEffect {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var seconds by remember { mutableStateOf("10") }
    var remainingTime by remember { mutableStateOf(0L) }
    var isAlarmRinging by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                remainingTime = intent?.getLongExtra(TimerService.TIME_ELAPSED, 0L) ?: 0L
            }
        }
        val alarmStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isAlarmRinging = intent?.getBooleanExtra(AlarmService.IS_RINGING, false) ?: false
            }
        }

        val timerFilter = IntentFilter(TimerService.TIMER_UPDATE)
        val alarmStateFilter = IntentFilter(AlarmService.ALARM_STATE_UPDATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(timerReceiver, timerFilter, Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(alarmStateReceiver, alarmStateFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(timerReceiver, timerFilter)
            context.registerReceiver(alarmStateReceiver, alarmStateFilter)
        }

        onDispose {
            context.unregisterReceiver(timerReceiver)
            context.unregisterReceiver(alarmStateReceiver)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isAlarmRinging -> "Time is up!"
                remainingTime > 0 -> "Time Remaining: $remainingTime s"
                else -> "Timer is not running"
            },
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Presets")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { startTimer(context, 300) }) { Text("5 Min") }
            Button(onClick = { startTimer(context, 600) }) { Text("10 Min") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { startTimer(context, 1260) }) { Text("21 Min") }
            Button(onClick = { startTimer(context, 3600) }) { Text("60 Min") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = seconds,
            onValueChange = { seconds = it },
            label = { Text("Custom (seconds)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { startTimer(context, seconds.toLongOrNull() ?: 10L) }) {
            Text("Start Custom Timer")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { Intent(context, TimerService::class.java).also { context.stopService(it) } }) {
            Text("Stop Timer")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { Intent(context, AlarmService::class.java).also { context.stopService(it) } }) {
            Text("Stop Alarm")
        }
    }
}

private fun startTimer(context: Context, durationInSeconds: Long) {
    Intent(context, TimerService::class.java).also {
        it.putExtra("TIMER_DURATION", durationInSeconds)
        context.startService(it)
    }
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreview() {
    AndroidTimerTheme {
        TimerScreen()
    }
}
