package com.example.kt4_14

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class CompassViewModel : ViewModel() {

    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> = _azimuth.asStateFlow()

    private val _hasSensor = MutableStateFlow(true)
    val hasSensor: StateFlow<Boolean> = _hasSensor.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    // Низкочастотный фильтр для сглаживания
    private val lowPassFilter = 0.15f

    // Предыдущие значения для фильтрации
    private var filteredGravity = FloatArray(3)
    private var filteredGeomagnetic = FloatArray(3)

    private val sensorListener = object : SensorEventListener {

        private val gravity = FloatArray(3)
        private val geomagnetic = FloatArray(3)
        private val rotationMatrix = FloatArray(9)
        private val orientation = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // Применяем низкочастотный фильтр
                    for (i in 0..2) {
                        filteredGravity[i] = if (filteredGravity[i] == 0f) {
                            event.values[i]
                        } else {
                            lowPassFilter * event.values[i] + (1 - lowPassFilter) * filteredGravity[i]
                        }
                        gravity[i] = filteredGravity[i]
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    // Применяем низкочастотный фильтр
                    for (i in 0..2) {
                        filteredGeomagnetic[i] = if (filteredGeomagnetic[i] == 0f) {
                            event.values[i]
                        } else {
                            lowPassFilter * event.values[i] + (1 - lowPassFilter) * filteredGeomagnetic[i]
                        }
                        geomagnetic[i] = filteredGeomagnetic[i]
                    }
                }
            }

            // Вычисляем ориентацию с отфильтрованными данными
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientation)

                var azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuthInDegrees = (azimuthInDegrees + 360) % 360

                viewModelScope.launch {
                    _azimuth.value = azimuthInDegrees
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                    _errorMessage.value = "Низкая точность компаса"
                }
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                    _errorMessage.value = "Калибруйте компас, водя телефоном по восьмерке"
                }
                else -> {
                    _errorMessage.value = null
                }
            }
        }
    }

    fun initSensors(context: Context) {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            if (accelerometer == null || magnetometer == null) {
                _hasSensor.value = false
                _errorMessage.value = "Устройство не поддерживает компас"
                return
            }

            _hasSensor.value = true

        } catch (e: Exception) {
            _hasSensor.value = false
            _errorMessage.value = "Ошибка инициализации сенсоров"
            Log.e("Compass", "Sensor init error", e)
        }
    }

    fun startListening() {
        filteredGravity = FloatArray(3)
        filteredGeomagnetic = FloatArray(3)

        accelerometer?.let {
            sensorManager?.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_UI  // Меньше обновлений = плавнее
            )
        }

        magnetometer?.let {
            sensorManager?.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(sensorListener)
    }

    fun getDirectionName(azimuth: Float): String {
        return when (azimuth) {
            in 337.5..360.0, in 0.0..22.5 -> "Север"
            in 22.5..67.5 -> "Северо-Восток"
            in 67.5..112.5 -> "Восток"
            in 112.5..157.5 -> "Юго-Восток"
            in 157.5..202.5 -> "Юг"
            in 202.5..247.5 -> "Юго-Запад"
            in 247.5..292.5 -> "Запад"
            in 292.5..337.5 -> "Северо-Запад"
            else -> "Север"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}