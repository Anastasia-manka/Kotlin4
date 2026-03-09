package com.example.kt4_9

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*

class WeatherForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private var instance: WeatherForegroundService? = null

        fun updateNotification(title: String, message: String) {
            instance?.updateNotificationInternal(title, message)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createChannel(this)

        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildNotification(this, "Сбор прогноза", "Подготовка...")
        )
    }

    private fun updateNotificationInternal(title: String, message: String) {
        val notification = NotificationHelper.buildNotification(this, title, message)
        val manager = NotificationManagerCompat.from(this)
        manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }
}