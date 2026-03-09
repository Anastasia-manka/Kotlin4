package com.example.kt4_7

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private var randomNumberService: RandomNumberService? = null

    private val _isBound = mutableStateOf(false)
    private val _currentNumber = mutableStateOf(0)

    private var pollingJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RandomNumberService.LocalBinder
            randomNumberService = binder.getService()
            _isBound.value = true
            startPolling()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Этот метод вызывается ТОЛЬКО при краше сервиса, не при нашем отключении
            randomNumberService = null
            _isBound.value = false
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()

        pollingJob = CoroutineScope(Dispatchers.Main).launch {
            while (_isBound.value) {
                _currentNumber.value = randomNumberService?.getCurrentNumber() ?: 0
                delay(1000)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RandomNumberScreen(
                currentNumber = _currentNumber.value,
                isConnected = _isBound.value,
                onConnect = { bindToService() },
                onDisconnect = { unbindFromService() }
            )
        }
    }

    private fun bindToService() {
        val intent = Intent(this, RandomNumberService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindFromService() {
        if (_isBound.value) {
            // 1. Останавливаем опрос
            stopPolling()

            // 2. Отвязываемся от сервиса
            unbindService(serviceConnection)

            // 3. ОБНОВЛЯЕМ СОСТОЯНИЕ СРАЗУ (не ждем onServiceDisconnected)
            randomNumberService = null
            _isBound.value = false

            // 4. НЕ сбрасываем _currentNumber - оставляем последнее значение
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        if (_isBound.value) {
            unbindService(serviceConnection)
        }
    }
}

@Composable
fun RandomNumberScreen(
    currentNumber: Int,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Случайное число",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "$currentNumber",
                fontSize = 72.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onConnect,
                enabled = !isConnected
            ) {
                Text("Подключиться")
            }

            Button(
                onClick = onDisconnect,
                enabled = isConnected
            ) {
                Text("Отключиться")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

    }
}