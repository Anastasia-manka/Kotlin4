package com.example.kt4_12

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FactsScreen()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FactsScreen(viewModel: FactsViewModel = viewModel()) {

    val uiState by viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Факты о животных",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(500)
                ) with
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(500)
                        )
            },
            label = "fact_animation"
        ) { state ->
            when (state) {
                is FactsUiState.Idle -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Нажмите кнопку, чтобы узнать факт",
                            modifier = Modifier.padding(24.dp),
                            fontSize = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                is FactsUiState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Загрузка интересного факта...",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                is FactsUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = state.fact,
                            modifier = Modifier.padding(24.dp),
                            fontSize = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                is FactsUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = state.message,
                            modifier = Modifier.padding(24.dp),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))


        Button(
            onClick = {
                when (uiState) {
                    is FactsUiState.Idle -> viewModel.loadNewFact()
                    is FactsUiState.Success -> viewModel.loadNewFact()
                    is FactsUiState.Error -> viewModel.loadNewFact()
                    else -> {}
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is FactsUiState.Loading
        ) {
            Text(
                when (uiState) {
                    is FactsUiState.Loading -> "Загрузка..."
                    else -> "Новый факт!"
                },
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}