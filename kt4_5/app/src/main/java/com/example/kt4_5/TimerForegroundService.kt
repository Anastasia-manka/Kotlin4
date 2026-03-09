package com.example.kt4_5

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*

class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var seconds = 0

    companion object {
        const val ACTION_COUNTER_UPDATE = "com.example.kt4_5.COUNTER_UPDATE"
        const val EXTRA_COUNTER_VALUE = "counter_value"
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        seconds = 0
        startTimer()

        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildNotification(this, seconds)
        )

        return START_STICKY
    }

    private fun startTimer() {
        serviceScope.launch {
            while (true) {
                delay(1000)
                seconds++


                val notification = NotificationHelper.buildNotification(this@TimerForegroundService, seconds)
                val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NotificationHelper.NOTIFICATION_ID, notification)


                sendCounterBroadcast(seconds)
            }
        }
    }

    private fun sendCounterBroadcast(value: Int) {
        val intent = Intent(ACTION_COUNTER_UPDATE).apply {
            putExtra(EXTRA_COUNTER_VALUE, value)
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}