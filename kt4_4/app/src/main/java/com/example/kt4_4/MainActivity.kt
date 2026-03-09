package com.example.kt4_4

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
import androidx.compose.ui.graphics.Color
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
import kotlin.random.Random
import kotlinx.coroutines.delay
import androidx.compose.foundation.background

//data класс для поста
data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String
)

//data класс для комментария
data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val body: String
)

//состояние загрузки для каждого поста
enum class LoadingState {
    LOADING,    //грузится
    READY,      //готово
    ERROR       //ошибка
}

//данные для отображения поста
data class PostWithData(
    val post: Post,
    val avatarColor: Color = Color.Gray,
    val comments: List<Comment> = emptyList(),
    val loadingState: LoadingState = LoadingState.LOADING
)

class SocialViewModel : ViewModel() {

    private val _postsWithData = MutableStateFlow<List<PostWithData>>(emptyList())
    val postsWithData: StateFlow<List<PostWithData>> = _postsWithData.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadingJob: Job? = null
    private var allPosts = listOf<Post>()
    private var allComments = listOf<Comment>()

    //загрузка постов и комментариев из JSON
    fun loadData(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //загружаем посты
                val postsJson = context.assets.open("posts.json")
                    .bufferedReader()
                    .use { it.readText() }
                val postsArray = JSONArray(postsJson)
                allPosts = (0 until postsArray.length()).map { i ->
                    val obj = postsArray.getJSONObject(i)
                    Post(
                        id = obj.getInt("id"),
                        userId = obj.getInt("userId"),
                        title = obj.getString("title"),
                        body = obj.getString("body")
                    )
                }

                //загружаем комментарии
                val commentsJson = context.assets.open("comments.json")
                    .bufferedReader()
                    .use { it.readText() }
                val commentsArray = JSONArray(commentsJson)
                allComments = (0 until commentsArray.length()).map { i ->
                    val obj = commentsArray.getJSONObject(i)
                    Comment(
                        postId = obj.getInt("postId"),
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        body = obj.getString("body")
                    )
                }

                println("загружено постов: ${allPosts.size}, комментариев: ${allComments.size}")

                //показываем посты в состоянии загрузки
                _postsWithData.value = allPosts.map { post ->
                    PostWithData(post = post, loadingState = LoadingState.LOADING)
                }

                //запускаем загрузку данных для каждого поста
                loadPostsData()

            } catch (e: IOException) {
                println("ошибка загрузки JSON: ${e.message}")
            } catch (e: Exception) {
                println("ошибка парсинга: ${e.message}")
            }
        }
    }

    //загрузка аватарок и комментариев для всех постов
    private fun loadPostsData() {
        //отменяем предыдущую загрузку если есть
        loadingJob?.cancel()

        loadingJob = viewModelScope.launch {
            _isRefreshing.value = true

            val currentPosts = _postsWithData.value

            //для каждого поста запускаем параллельную загрузку
            val deferredResults = currentPosts.mapIndexed { index, postWithData ->
                async {
                    try {
                        //загружаем аватарку
                        val avatarColor = withContext(Dispatchers.IO) {
                            Thread.sleep((1000 + Random.nextInt(1000)).toLong())
                            Color(
                                red = Random.nextFloat(),
                                green = Random.nextFloat(),
                                blue = Random.nextFloat(),
                                alpha = 1f
                            )
                        }

                        //загружаем комментарии
                        val comments = withContext(Dispatchers.IO) {
                            Thread.sleep((800 + Random.nextInt(1200)).toLong())
                            allComments.filter { it.postId == postWithData.post.id }
                        }

                        //возвращаем обновленный пост
                        postWithData.copy(
                            avatarColor = avatarColor,
                            comments = comments,
                            loadingState = LoadingState.READY
                        )
                    } catch (e: Exception) {
                        println("ошибка загрузки для поста ${postWithData.post.id}: ${e.message}")
                        postWithData.copy(
                            loadingState = LoadingState.ERROR
                        )
                    }
                }
            }

            //ждем завершения всех загрузок
            val updatedPosts = deferredResults.awaitAll()

            _postsWithData.value = updatedPosts
            _isRefreshing.value = false
        }
    }

    //обновление ленты (отмена и повторная загрузка)
    fun refresh() {
        println("обновление ленты")
        loadPostsData()
    }
}

@Composable
fun SocialFeedScreen(viewModel: SocialViewModel = viewModel()) {
    val postsWithData by viewModel.postsWithData.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current

    //загружаем данные при первом запуске
    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        //заголовок и кнопка обновить
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Социальная лента",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = { viewModel.refresh() },
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("обновить")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        //список постов
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(postsWithData) { postWithData ->
                PostCard(postWithData = postWithData)
            }
        }
    }
}

@Composable
fun PostCard(postWithData: PostWithData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            //верхняя строка с "аватаркой" и заголовком
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                //цветной кружок вместо аватарки
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            when (postWithData.loadingState) {
                                LoadingState.LOADING -> Color.LightGray
                                LoadingState.READY -> postWithData.avatarColor
                                LoadingState.ERROR -> Color.Red.copy(alpha = 0.3f)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                //заголовок поста
                Column {
                    Text(
                        text = postWithData.post.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "пользователь ${postWithData.post.userId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            //текст поста
            Text(
                text = postWithData.post.body,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            //блок комментариев
            when (postWithData.loadingState) {
                LoadingState.LOADING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "загрузка комментариев...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                LoadingState.READY -> {
                    if (postWithData.comments.isEmpty()) {
                        Text(
                            text = "нет комментариев",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "комментарии (${postWithData.comments.size}):",
                                style = MaterialTheme.typography.titleSmall
                            )

                            postWithData.comments.take(3).forEach { comment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.LightGray.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = comment.name,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = comment.body,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            if (postWithData.comments.size > 3) {
                                Text(
                                    text = "еще ${postWithData.comments.size - 3} комментариев...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                LoadingState.ERROR -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ошибка загрузки",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                SocialFeedScreen()
            }
        }
    }
}