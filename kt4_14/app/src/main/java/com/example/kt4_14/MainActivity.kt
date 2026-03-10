package com.example.kt4_14

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext  // ← добавить этот импорт
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompassScreen()
        }
    }
}

@Composable
fun CompassScreen(viewModel: CompassViewModel = viewModel()) {

    val azimuth by viewModel.azimuth.collectAsStateWithLifecycle()
    val hasSensor by viewModel.hasSensor.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Управление жизненным циклом сенсоров
    DisposableEffect(Unit) {
        viewModel.initSensors(context)
        viewModel.startListening()

        onDispose {
            viewModel.stopListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Компас",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!hasSensor) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage ?: "Устройство не поддерживает компас",
                    modifier = Modifier.padding(24.dp),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // Компас
            CompassView(azimuth = azimuth)

            Spacer(modifier = Modifier.height(80.dp))

            // Азимут
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Азимут",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${azimuth.toInt()}°",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = viewModel.getDirectionName(azimuth),
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = errorMessage!!,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}