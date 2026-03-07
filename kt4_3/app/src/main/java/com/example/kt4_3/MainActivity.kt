package com.example.kt4_3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.IOException


data class Repository(
    val id: Int,
    val full_name: String,
    val description: String?,
    val stargazers_count: Int,
    val language: String?
)

class SearchViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Repository>>(emptyList())
    val searchResults: StateFlow<List<Repository>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null
    private val allRepos = mutableListOf<Repository>()

    fun loadRepositoriesFromAssets(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("github_repos.json")
                    .bufferedReader()
                    .use { it.readText() }

                //парсим JSON вручную через org.json
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val repo = Repository(
                        id = obj.getInt("id"),
                        full_name = obj.getString("full_name"),
                        description = obj.optString("description", null),
                        stargazers_count = obj.getInt("stargazers_count"),
                        language = obj.optString("language", null)
                    )
                    allRepos.add(repo)
                }
                println("загружено репозиториев: ${allRepos.size}")
            } catch (e: IOException) {
                println("ошибка загрузки JSON: ${e.message}")
            } catch (e: Exception) {
                println("ошибка парсинга: ${e.message}")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(500)

            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            println("поиск: $query")

            delay(800)

            val results = allRepos.filter { repo ->
                repo.full_name.contains(query, ignoreCase = true) ||
                        (repo.description?.contains(query, ignoreCase = true) == true)
            }

            _searchResults.value = results
            _isLoading.value = false
            println("найдено: ${results.size}")
        }
    }
}

@Composable
fun SearchScreen(viewModel: SearchViewModel = viewModel()) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()


    val context = LocalContext.current  //получаем здесь
    LaunchedEffect(Unit) {
        viewModel.loadRepositoriesFromAssets(context) //используем здесь
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("поиск репозиториев...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults) { repo ->
                RepositoryCard(repo = repo)
            }
        }
    }
}

@Composable
fun RepositoryCard(repo: Repository) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = repo.full_name,
                style = MaterialTheme.typography.titleMedium
            )

            if (!repo.description.isNullOrBlank()) {
                Text(
                    text = repo.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!repo.language.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = repo.language!!,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Text(
                    text = "звезд: " + repo.stargazers_count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val viewModel: SearchViewModel = viewModel()
                SearchScreen(viewModel = viewModel)
            }
        }
    }
}