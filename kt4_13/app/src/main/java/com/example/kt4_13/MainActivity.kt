package com.example.kt4_13

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CurrencyScreen()
        }
    }
}

@Composable
fun CurrencyScreen(viewModel: CurrencyViewModel = viewModel()) {

    val rate by viewModel.rate.collectAsStateWithLifecycle()
    val direction by viewModel.direction.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Анимация поворота стрелки
    val rotation by animateFloatAsState(
        targetValue = when (direction) {
            RateChange.UP -> 0f
            RateChange.DOWN -> 180f
            RateChange.NO_CHANGE -> 0f
        },
        animationSpec = tween(300),
        label = "arrow_rotation"
    )

    // Цвет стрелки
    val arrowColor = when (direction) {
        RateChange.UP -> Color.Green
        RateChange.DOWN -> Color.Red
        RateChange.NO_CHANGE -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Курс USD/RUB",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.2f".format(rate),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(rotation),
                    tint = arrowColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))



        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.refreshRate() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRefreshing
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (isRefreshing) "Обновление..." else "Обновить сейчас",
                fontSize = 18.sp
            )
        }
    }
}