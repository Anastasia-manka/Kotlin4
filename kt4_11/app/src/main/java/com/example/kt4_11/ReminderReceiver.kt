package com.example.kt4_11

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Будильник сработал!")

        ReminderNotificationHelper.createChannel(context)

        if (hasNotificationPermission(context)) {
            showNotification(context)
        } else {
            Log.e("ReminderReceiver", "Нет разрешения на уведомления")
        }

        ReminderManager.scheduleNextReminder(context)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showNotification(context: Context) {
        try {
            val notification = ReminderNotificationHelper.buildNotification(context)
            NotificationManagerCompat.from(context).notify(
                ReminderNotificationHelper.NOTIFICATION_ID,
                notification
            )
            Log.d("ReminderReceiver", "Уведомление показано")
        } catch (e: SecurityException) {
            Log.e("ReminderReceiver", "Ошибка показа уведомления", e)
        }
    }
}