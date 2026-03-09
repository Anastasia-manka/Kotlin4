package com.example.kt4_5

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*

class TimerForegroundService : Service() {

    private var serviceScope: CoroutineScope? = null
    private var seconds = 0

    companion object {
        const val ACTION_COUNTER_UPDATE = "com.example.kt4_5.COUNTER_UPDATE"
        const val EXTRA_COUNTER_VALUE = "counter_value"
        const val ACTION_COUNTER_RESET = "com.example.kt4_5.COUNTER_RESET"
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        stopTimer()

        seconds = 0

        sendResetBroadcast()

        updateNotification(seconds)

        startTimer()

        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildNotification(this, seconds)
        )

        return START_STICKY
    }

    private fun startTimer() {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        serviceScope = scope

        scope.launch {
            while (true) {
                delay(1000)
                seconds++

                updateNotification(seconds)
                sendCounterBroadcast(seconds)
            }
        }
    }

    private fun stopTimer() {
        serviceScope?.cancel()
        serviceScope = null
    }

    private fun updateNotification(value: Int) {
        val notification = NotificationHelper.buildNotification(this, value)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun sendCounterBroadcast(value: Int) {
        val intent = Intent(ACTION_COUNTER_UPDATE).apply {
            putExtra(EXTRA_COUNTER_VALUE, value)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendResetBroadcast() {
        val intent = Intent(ACTION_COUNTER_RESET)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        sendResetBroadcast()
    }
}