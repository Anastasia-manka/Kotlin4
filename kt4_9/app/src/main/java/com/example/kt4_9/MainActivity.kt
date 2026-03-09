package com.example.kt4_9

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider  // ← ЭТОТ ИМПОРТ НУЖЕН
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    private lateinit var weatherViewModel: WeatherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        weatherViewModel = ViewModelProvider(
            this,
            WeatherViewModelFactory(application)
        ).get(WeatherViewModel::class.java)

        setContent {
            WeatherScreen(weatherViewModel)
        }
    }
}

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {

    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Прогноз погоды",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = when (weatherState) {
                    is WeatherState.Idle -> "Нажмите кнопку для сбора прогноза"
                    is WeatherState.Loading -> "Собираем данные о погоде..."
                    is WeatherState.Success -> (weatherState as WeatherState.Success).report
                    is WeatherState.Error -> "Ошибка: ${(weatherState as WeatherState.Error).message}"
                },
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                when (weatherState) {
                    is WeatherState.Idle -> viewModel.startWeatherCollection()
                    is WeatherState.Success -> viewModel.resetState()
                    is WeatherState.Error -> viewModel.resetState()
                    else -> {}
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = weatherState !is WeatherState.Loading
        ) {
            Text(
                when (weatherState) {
                    is WeatherState.Idle -> "Собрать прогноз"
                    is WeatherState.Loading -> "Сбор..."
                    is WeatherState.Success -> "Начать заново"
                    is WeatherState.Error -> "Попробовать снова"
                }
            )
        }
    }
}