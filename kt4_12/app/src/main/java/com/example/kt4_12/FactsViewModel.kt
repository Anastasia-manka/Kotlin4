package com.example.kt4_12

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class FactsViewModel : ViewModel() {


    private val _uiState = mutableStateOf<FactsUiState>(FactsUiState.Idle)
    val uiState: State<FactsUiState> = _uiState

    private val facts = listOf(
        "Кошки спят около 16 часов в день",
        "Слоны — единственные млекопитающие, которые не умеют прыгать",
        "Осьминоги имеют три сердца",
        "Жирафы спят всего 2 часа в день",
        "Дельфины спят с одним открытым глазом",
        "Собаки понимают до 250 слов и жестов",
        "Панды едят до 38 кг бамбука в день",
        "Колибри делают до 80 взмахов крыльями в секунду",
        "Львы спят около 20 часов в сутки",
        "Белые медведи имеют черную кожу под белым мехом",
        "Ленивцы могут задерживать дыхание на 40 минут",
        "Вороны способны запоминать лица людей",
        "Кенгуру не умеют ходить задом",
        "Улитки могут спать 3 года",
        "Креветки бьют своих жертв с силой пули"
    )

    /**
     * Cold Flow: при каждом вызове создается новый поток
     * с задержкой и случайным фактом
     */
    fun getRandomFact(): Flow<String> = flow {
        val delayTime = Random.nextLong(1500, 3000)
        delay(delayTime)

        val randomFact = facts.random()
        emit(randomFact)
    }

    /**
     * Загрузка нового факта
     */
    fun loadNewFact() {
        _uiState.value = FactsUiState.Loading

        viewModelScope.launch {
            try {
                getRandomFact().collect { fact ->
                    _uiState.value = FactsUiState.Success(fact)
                }
            } catch (e: Exception) {
                _uiState.value = FactsUiState.Error("Ошибка загрузки: ${e.message}")
            }
        }
    }

    /**
     * Сброс состояния (для повторной загрузки)
     */
    fun resetState() {
        _uiState.value = FactsUiState.Idle
    }
}


sealed class FactsUiState {
    object Idle : FactsUiState()
    object Loading : FactsUiState()
    data class Success(val fact: String) : FactsUiState()
    data class Error(val message: String) : FactsUiState()
}