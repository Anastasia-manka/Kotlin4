package com.example.kt4_9

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

sealed class WeatherState {
    object Idle : WeatherState()
    object Loading : WeatherState()
    data class Success(val report: String) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val workManager = WorkManager.getInstance(context)

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weatherState: StateFlow<WeatherState> = _weatherState

    private val cities = listOf("Москва", "Лондон", "Нью-Йорк", "Токио")

    fun startWeatherCollection() {
        _weatherState.value = WeatherState.Loading

        val serviceIntent = Intent(context, WeatherForegroundService::class.java)
        context.startForegroundService(serviceIntent)

        WeatherForegroundService.updateNotification(
            "Сбор прогноза",
            "Загружаем погоду для ${cities.size} городов..."
        )

        val weatherWorkers = cities.mapIndexed { index, city ->
            OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(workDataOf(KEY_CITY to city))
                .addTag("weather_$index")
                .build()
        }

        val reportWorker = OneTimeWorkRequestBuilder<WeatherReportWorker>()
            .build()

        val continuation = workManager
            .beginWith(weatherWorkers)
            .then(reportWorker)

        continuation.enqueue()

        observeWorkProgress(weatherWorkers, reportWorker)
    }

    private fun observeWorkProgress(
        weatherWorkers: List<OneTimeWorkRequest>,
        reportWorker: OneTimeWorkRequest
    ) {
        weatherWorkers.forEachIndexed { index, work ->
            viewModelScope.launch {
                workManager.getWorkInfoByIdFlow(work.id).collect { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val city = workInfo.outputData.getString(KEY_CITY) ?: "Город"
                            val temp = workInfo.outputData.getInt(KEY_TEMPERATURE, 0)

                            val completedCount = weatherWorkers.count { worker ->
                                runCatching {
                                    workManager.getWorkInfoById(worker.id).get().state == WorkInfo.State.SUCCEEDED
                                }.getOrDefault(false)
                            } + 1

                            val doneCities = cities.take(completedCount).joinToString()
                            val remaining = cities.size - completedCount

                            val message = if (remaining > 0) {
                                "Готово: $doneCities, осталось $remaining"
                            } else {
                                "Все города готовы, формируем отчет..."
                            }

                            WeatherForegroundService.updateNotification(
                                "Сбор прогноза",
                                message
                            )

                            Log.d("VIEWMODEL", "$city: $temp°C")
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(KEY_ERROR) ?: "Ошибка"
                            _weatherState.value = WeatherState.Error(error)

                            WeatherForegroundService.updateNotification(
                                "Ошибка",
                                error
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(reportWorker.id).collect { workInfo ->
                Log.d("VIEWMODEL", "Report worker state: ${workInfo.state}, output: ${workInfo.outputData.keyValueMap}")

                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        WeatherForegroundService.updateNotification(
                            "Сбор прогноза",
                            "Формируем итоговый отчет..."
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val report = workInfo.outputData.getString(KEY_FINAL_REPORT)
                            ?: "Отчет готов"
                        val avgTemp = workInfo.outputData.getInt(KEY_TEMPERATURE, 0)

                        Log.d("VIEWMODEL", "Report succeeded: $report")
                        _weatherState.value = WeatherState.Success(report)

                        WeatherForegroundService.updateNotification(
                            "Отчет готов!",
                            "Средняя температура: $avgTemp°C"
                        )

                        viewModelScope.launch {
                            delay(3000)
                            context.stopService(Intent(context, WeatherForegroundService::class.java))
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(KEY_ERROR) ?: "Ошибка"
                        Log.e("VIEWMODEL", "Report failed: $error")
                        _weatherState.value = WeatherState.Error(error)

                        WeatherForegroundService.updateNotification(
                            "Ошибка",
                            error
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun resetState() {
        _weatherState.value = WeatherState.Idle
    }
}