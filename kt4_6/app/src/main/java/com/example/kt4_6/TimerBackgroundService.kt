package com.example.kt4_6

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*

class TimerBackgroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val EXTRA_SECONDS = "extra_seconds"
        const val ACTION_TIMER_FINISHED = "com.example.kt4_6.TIMER_FINISHED"
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0

        if (seconds > 0) {
            startTimer(seconds)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        serviceScope.launch {
            delay(seconds * 1000L)

            // Показываем уведомление
            showNotification()

            // Отправляем сигнал в Activity
            sendFinishBroadcast()

            // Останавливаем сервис
            stopSelf()
        }
    }

    private fun showNotification() {
        val notification = NotificationHelper.buildFinishNotification(this)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun sendFinishBroadcast() {
        val intent = Intent(ACTION_TIMER_FINISHED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}