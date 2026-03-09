package com.example.kt4_9

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random
import com.example.kt4_9.*

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val city = inputData.getString(KEY_CITY) ?: return Result.failure(
            workDataOf(KEY_ERROR to "Не указан город")
        )

        return try {
            Log.d("WORKER", "Начинаем загрузку для $city")

            // Имитация загрузки
            for (i in 1..100 step 20) {
                if (!coroutineContext.isActive) return Result.failure()
                setProgress(workDataOf(KEY_CITY to city, KEY_PROGRESS to i))
                delay(500)
            }

            val temperature = Random.nextInt(-5, 25)
            Log.d("WORKER", "Загрузка для $city завершена: $temperature°C")

            val id = city.hashCode().toString()

            Result.success(workDataOf(
                "city_$id" to city,        // Уникальный ключ для города
                "temp_$id" to temperature,  // Уникальный ключ для температуры
                KEY_CITY to city,            // Для обратной совместимости
                KEY_TEMPERATURE to temperature
            ))

        } catch (e: Exception) {
            Log.e("WORKER", "Ошибка загрузки для $city", e)
            Result.failure(workDataOf(
                KEY_CITY to city,
                KEY_ERROR to "Ошибка: ${e.message}"
            ))
        }
    }
}