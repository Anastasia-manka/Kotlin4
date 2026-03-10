package com.example.kt4_11

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background

class MainActivity : ComponentActivity() {

    private val _isEnabled = mutableStateOf(false)
    private val _nextTime = mutableStateOf("")

    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkExactAlarmPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _isEnabled.value = ReminderManager.isReminderEnabled(this)
        updateNextTime()

        setContent {
            MainScreen(
                isEnabled = _isEnabled.value,
                nextTime = _nextTime.value,
                onToggle = { enable ->
                    if (enable) {
                        enableReminder()
                    } else {
                        disableReminder()
                    }
                }
            )
        }
    }

    private fun enableReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = android.content.Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:$packageName")
                )
                exactAlarmPermissionLauncher.launch(intent)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    doEnableReminder()
                }
            }
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            doEnableReminder()
        }
    }

    private fun doEnableReminder() {
        ReminderManager.enableReminder(this)
        _isEnabled.value = true
        updateNextTime()
    }

    private fun disableReminder() {
        ReminderManager.disableReminder(this)
        _isEnabled.value = false
        updateNextTime()
    }

    private fun updateNextTime() {
        _nextTime.value = if (_isEnabled.value) {
            "Следующее напоминание: ${ReminderManager.getNextReminderTime(this)}"
        } else {
            "Напоминание выключено"
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (alarmManager.canScheduleExactAlarms() && _isEnabled.value) {
                // Если разрешение получено - включаем
                doEnableReminder()
            }
        }
    }
}

@Composable
fun MainScreen(
    isEnabled: Boolean,
    nextTime: String,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Напоминание о таблетке",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )


        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (isEnabled) Color.Green else Color.Gray
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEnabled) "Вкл" else "Выкл",
                color = Color.White,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))


        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = nextTime,
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onToggle(!isEnabled) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (isEnabled) "Выключить напоминание" else "Включить напоминание",
                fontSize = 18.sp
            )
        }
    }
}