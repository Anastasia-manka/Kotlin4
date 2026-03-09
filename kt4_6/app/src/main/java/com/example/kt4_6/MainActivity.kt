package com.example.kt4_6

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

    // Состояние для отслеживания работы таймера
    private val _isRunning = mutableStateOf(false)

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerBackgroundService.ACTION_TIMER_FINISHED) {
                _isRunning.value = false  // Таймер завершился - кнопка снова активна
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TimerScreen(
                isRunning = _isRunning.value,
                onStartTimer = { seconds ->
                    _isRunning.value = true  // Таймер запущен - кнопка неактивна
                    val intent = Intent(this, TimerBackgroundService::class.java).apply {
                        putExtra(TimerBackgroundService.EXTRA_SECONDS, seconds)
                    }
                    startService(intent)
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            finishReceiver,
            IntentFilter(TimerBackgroundService.ACTION_TIMER_FINISHED)
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver)
    }
}

@Composable
fun TimerScreen(
    isRunning: Boolean,
    onStartTimer: (Int) -> Unit
) {
    val context = LocalContext.current
    var secondsInput by remember { mutableStateOf("") }

    // Преобразуем ввод в число для проверки
    val seconds = secondsInput.toIntOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Одноразовый таймер",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Поле ввода - неактивно, если таймер работает
        OutlinedTextField(
            value = secondsInput,
            onValueChange = { secondsInput = it },
            label = { Text("Введите секунды") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning  // Неактивно во время работы таймера
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка запуска
        Button(
            onClick = {
                seconds?.let {
                    onStartTimer(it)
                    secondsInput = ""  // Очищаем поле после запуска
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning && (seconds != null && seconds > 0)  // Неактивно во время работы
        ) {
            Text("Запустить таймер")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ИНДИКАТОР РАБОТЫ ТАЙМЕРА
        if (isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Таймер работает...",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}