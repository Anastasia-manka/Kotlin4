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

    /**
     * Локальный Binder - возвращает сам сервис
     * Чтобы Activity могла вызывать его методы
     */
    inner class LocalBinder : Binder() {
        fun getService(): RandomNumberService = this@RandomNumberService
    }

    override fun onCreate() {
        super.onCreate()
        startGenerating()
    }

    /**
     * Запуск генерации случайных чисел
     */
    private fun startGenerating() {
        if (isGenerating) return

        isGenerating = true
        serviceScope.launch {
            while (isGenerating) {
                delay(1000)  // Каждую секунду
                currentNumber = (0..100).random()  // Случайное число 0-100
            }
        }
    }

    /**
     * Остановка генерации
     */
    private fun stopGenerating() {
        isGenerating = false
    }

    /**
     * Публичный метод для получения текущего числа
     * Activity будет вызывать его каждую секунду
     */
    fun getCurrentNumber(): Int = currentNumber

    override fun onBind(intent: Intent?): IBinder {
        return binder  // Возвращаем Binder при привязке
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopGenerating()  // Останавливаем генерацию, если никто не привязан
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGenerating()
        serviceScope.cancel()
    }
}