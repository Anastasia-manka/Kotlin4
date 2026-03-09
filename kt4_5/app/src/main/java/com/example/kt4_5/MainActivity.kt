package com.example.kt4_5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : ComponentActivity() {

    private val _counter = mutableStateOf(0)

    private val counterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerForegroundService.ACTION_COUNTER_UPDATE -> {
                    val value = intent.getIntExtra(TimerForegroundService.EXTRA_COUNTER_VALUE, 0)
                    _counter.value = value
                }
                TimerForegroundService.ACTION_COUNTER_RESET -> {
                    _counter.value = 0
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TimerScreen(counter = _counter.value)
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            counterReceiver,
            IntentFilter().apply {
                addAction(TimerForegroundService.ACTION_COUNTER_UPDATE)
                addAction(TimerForegroundService.ACTION_COUNTER_RESET)
            }
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(counterReceiver)
    }
}

@Composable
fun TimerScreen(counter: Int) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$counter",
            fontSize = 72.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                val intent = Intent(context, TimerForegroundService::class.java)
                context.startForegroundService(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Старт")
        }

        Button(
            onClick = {
                val intent = Intent(context, TimerForegroundService::class.java)
                context.stopService(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Стоп")
        }
    }
}