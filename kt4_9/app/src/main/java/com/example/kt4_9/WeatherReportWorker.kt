package com.example.kt4_9

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import com.example.kt4_9.*

class WeatherReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WORKER", "Формирование отчета. Входные данные: ${inputData.keyValueMap}")

            val allData = inputData.keyValueMap

            val cities = mutableListOf<String>()
            val temperatures = mutableListOf<Int>()

            allData.forEach { (key, value) ->
                when {
                    // Если ключ содержит "city_" - это название города
                    key.startsWith("city_") && value is String -> {
                        val city = value
                        val index = key.removePrefix("city_")

                        // Ищем соответствующую температуру
                        val tempKey = "temp_$index"
                        val temp = allData[tempKey] as? Int

                        if (temp != null) {
                            cities.add(city)
                            temperatures.add(temp)
                            Log.d("WORKER", "Найден город: $city, температура: $temp")
                        }
                    }
                }
            }

            if (cities.isEmpty()) {
                val city = allData[KEY_CITY] as? String
                val temp = allData[KEY_TEMPERATURE] as? Int

                if (city != null && temp != null) {
                    cities.add(city)
                    temperatures.add(temp)
                    Log.d("WORKER", "Найден одиночный город: $city, температура: $temp")
                }
            }

            if (cities.isEmpty()) {
                Log.e("WORKER", "Не найдено данных о городах. Все ключи: ${allData.keys}")
                return Result.failure(workDataOf(KEY_ERROR to "Нет данных о городах. Получены ключи: ${allData.keys}"))
            }

            val averageTemp = temperatures.average().toInt()

            // Формируем отчет
            val report = buildString {
                append("Средняя температура: $averageTemp°C\n")
                append("По городам:\n")
                cities.zip(temperatures).forEach { (city, temp) ->
                    append("• $city: $temp°C\n")
                }
            }

            Log.d("WORKER", "Отчет готов: $report")

            delay(1000)

            Result.success(workDataOf(
                KEY_FINAL_REPORT to report,
                KEY_TEMPERATURE to averageTemp
            ))

        } catch (e: Exception) {
            Log.e("WORKER", "Ошибка формирования отчета", e)
            Result.failure(workDataOf(KEY_ERROR to "Ошибка отчета: ${e.message}"))
        }
    }
}