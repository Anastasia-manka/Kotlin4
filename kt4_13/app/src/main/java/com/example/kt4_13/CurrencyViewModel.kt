package com.example.kt4_13

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class CurrencyViewModel : ViewModel() {

    // StateFlow для хранения текущего курса
    private val _rate = MutableStateFlow(95.5)
    val rate: StateFlow<Double> = _rate.asStateFlow()

    // Для определения направления изменения
    private val _direction = MutableStateFlow(RateChange.NO_CHANGE)
    val direction: StateFlow<RateChange> = _direction.asStateFlow()

    // Флаг для индикации обновления
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        startAutoUpdate()
    }

    private fun startAutoUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                generateNewRate()
            }
        }
    }

    fun refreshRate() {
        viewModelScope.launch {
            _isRefreshing.value = true
            generateNewRate()
            delay(500)
            _isRefreshing.value = false
        }
    }

    private fun generateNewRate() {
        val currentRate = _rate.value
        val change = Random.nextDouble(-2.0, 2.0)
        val newRate = (currentRate + change).coerceIn(85.0, 105.0)

        // Определяем направление ДО обновления _rate
        _direction.value = when {
            newRate > currentRate -> RateChange.UP
            newRate < currentRate -> RateChange.DOWN
            else -> RateChange.NO_CHANGE
        }

        // Обновляем курс
        _rate.value = newRate
    }
}

enum class RateChange {
    UP, DOWN, NO_CHANGE
}