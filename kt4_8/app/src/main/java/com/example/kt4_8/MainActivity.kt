package com.example.kt4_8

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {

    private lateinit var photoViewModel: PhotoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        photoViewModel = ViewModelProvider(
            this,
            PhotoViewModelFactory(application)
        ).get(PhotoViewModel::class.java)

        setContent {
            PhotoProcessingScreen(photoViewModel)
        }
    }
}

@Composable
fun PhotoProcessingScreen(viewModel: PhotoViewModel) {

    val processingState by viewModel.processingState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Обработка фото",
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
                text = when (processingState) {
                    is ProcessingState.Idle -> "Нажмите кнопку для старта"
                    is ProcessingState.Compressing -> "Сжимаем фото..."
                    is ProcessingState.Watermarking -> "Добавляем водяной знак..."
                    is ProcessingState.Uploading -> "Загружаем в облако..."
                    is ProcessingState.Success -> (processingState as ProcessingState.Success).message
                    is ProcessingState.Error -> (processingState as ProcessingState.Error).message
                },
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (processingState !is ProcessingState.Idle &&
            processingState !is ProcessingState.Success &&
            processingState !is ProcessingState.Error) {

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$progress%",
                modifier = Modifier.align(Alignment.End)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                when (processingState) {
                    is ProcessingState.Idle -> viewModel.startProcessing()
                    is ProcessingState.Success -> viewModel.resetState()
                    is ProcessingState.Error -> viewModel.resetState()
                    else -> {}
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = processingState is ProcessingState.Idle ||
                    processingState is ProcessingState.Success ||
                    processingState is ProcessingState.Error
        ) {
            Text(
                when (processingState) {
                    is ProcessingState.Idle -> "Начать обработку"
                    is ProcessingState.Success -> "Начать заново"
                    is ProcessingState.Error -> "Попробовать снова"
                    else -> "Обработка..."
                }
            )
        }
    }
}