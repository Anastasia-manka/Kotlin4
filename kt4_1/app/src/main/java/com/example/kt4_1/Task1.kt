package com.example.kt4_1

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis
import java.io.File
import kotlin.random.Random

//класс для пользователя
@Serializable
data class User(
    val id: Int,
    val name: String
)

//класс для одной позиции в продажах
@Serializable
data class SaleItem(
    val product: String,
    val qty: Int,
    val revenue: Int
)

//класс для всего ответа с продажами
@Serializable
data class SalesResponse(
    val today: String,
    val items: List<SaleItem>
)

//класс для погоды
@Serializable
data class Weather(
    val city: String,
    val temp: Int,
    val condition: String
)
 //функция чтения файла
fun readFileFromResources(fileName: String): String {
        val resourcePath = "app/src/main/resources/$fileName"
    val file = File(resourcePath)
    //читаем файл и возвращаем его содержимое как строку
    return file.readText()
}
//загрузка списка пользователей
suspend fun loadUsers(): List<String>? {
    return try {
        println("Loading users...")
        delay(1800)

        //читаем файл и парсим JSON в список объектов User
        val jsonString = readFileFromResources("users.json")
        val users = Json.decodeFromString<List<User>>(jsonString)

        //имитация случайной ошибки (с вероятностью 50%)
        if (Random.nextBoolean()) {
            throw Exception("Error when uploading users")
        }

        println("Users loaded successfully")
        users.map { it.name }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        null
    }
}

//загрузка данных о продажах
suspend fun loadSales(): Map<String, Int>? {
    return try {
        println("Loading sales..")
        delay(1200)

        val jsonString = readFileFromResources("sales.json")
        val sales = Json.decodeFromString<SalesResponse>(jsonString)

        if (Random.nextBoolean()) {
            throw Exception("Error when uploading sales")
        }

        println("Sales loaded successfully")
        sales.items.associate { it.product to it.qty }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        null
    }
}

suspend fun loadWeather(): List<String>? {
    return try {
        println("Loading weather...")
        delay(2500)

        val jsonString = readFileFromResources("weather.json")
        val weatherList = Json.decodeFromString<List<Weather>>(jsonString)

        if (Random.nextBoolean()) {
            throw Exception("Error loading the weather")
        }

        println("Weather loaded successfully")
        weatherList.map { "${it.city}: ${it.temp}C" }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        null
    }
}

fun main() {
    println("Program started")
    println("Starting parallel data loading")
    println()

    //замеряем время
    val totalTime = measureTimeMillis {
        runBlocking {
            val usersDeferred = async { loadUsers() }
            val salesDeferred = async { loadSales() }
            val weatherDeferred = async { loadWeather() }


            val users = usersDeferred.await()
            val sales = salesDeferred.await()
            val weather = weatherDeferred.await()

            println()
            println("RESULTS:")


            // Выводим результаты, проверяя, не было ли ошибок (null)
            println()
            println("Users:")
            if (users != null) {
                println("   $users")
            } else {
                println("   Failed to load data")
            }

            println()
            println("Sales:")
            if (sales != null) {
                sales.forEach { (product, qty) ->
                    println("   $product: $qty pieces")
                }
            } else {
                println("   Failed to load data")
            }

            println()
            println("Weather:")
            if (weather != null) {
                weather.forEach { cityWeather ->
                    println("   $cityWeather")
                }
            } else {
                println("   Failed to load data")
            }
        }
    }

    println()
    println("==================================================")
    println("Total lead time: ${totalTime / 1000.0} seconds")
}
