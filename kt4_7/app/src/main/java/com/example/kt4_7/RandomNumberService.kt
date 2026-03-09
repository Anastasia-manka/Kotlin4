package com.example.kt4_7

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*

class RandomNumberService : Service() {

    // Binder для клиентов (Activity)
    private val binder = LocalBinder()

    // Coroutine scope для фоновой генерации чисел
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Текущее случайное число
    private var currentNumber = 0

    // Флаг работы генерации
    private var isGenerating = false


    inner class LocalBinder : Binder() {
        fun getService(): RandomNumberService = this@RandomNumberService
    }

    override fun onCreate() {
        super.onCreate()
        startGenerating()
    }


    private fun startGenerating() {
        if (isGenerating) return

        isGenerating = true
        serviceScope.launch {
            while (isGenerating) {
                delay(1000)
                currentNumber = (0..100).random()
            }
        }
    }


    private fun stopGenerating() {
        isGenerating = false
    }


    fun getCurrentNumber(): Int = currentNumber

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopGenerating()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGenerating()
        serviceScope.cancel()
    }
}