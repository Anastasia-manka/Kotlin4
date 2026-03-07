package com.example.kt4_2

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis
import java.io.File
import java.security.MessageDigest

//функция вычисляет SHA-256 хеш файла
suspend fun calculateSHA256(file: File): String {
    return withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        digest.joinToString("") { "%02x".format(it) }
    }
}

//функция ищет все .json файлы в папке и подпапках
fun findJsonFiles(rootDir: String): List<File> {
    val root = File(rootDir)
    return root.walk()
        .filter { it.isFile }
        .filter { it.extension == "json" }
        .toList()
}

fun main() {
    val filesDirectory = "app/src/main/resources/files"

    println("Search for JSON files in: $filesDirectory")
    println()

    val totalTime = measureTimeMillis {
        runBlocking {
            withTimeoutOrNull(5000) {
                val jsonFiles = findJsonFiles(filesDirectory)
                println("files found: ${jsonFiles.size}")
                println()

                if (jsonFiles.isEmpty()) {
                    println("no json files found")
                    return@withTimeoutOrNull
                }

                //для каждого файла вычисляем хеш параллельно
                val deferredResults = jsonFiles.map { file ->
                    async {
                        file to calculateSHA256(file)
                    }
                }

                val results = deferredResults.awaitAll()

                val groups = results
                    .groupBy({ it.second }, { it.first })  //группируем: хеш -> список файлов
                    .filter { it.value.size > 1 }  //оставляем только дубликаты (больше 1 файла)

                if (groups.isEmpty()) {
                    println("no duplicates found")
                } else {
                    println("duplicates found:")
                    println()

                    groups.forEach { (hash, files) ->
                        println("hash: $hash")
                        files.forEach { file ->
                            println("  ${file.path}")
                        }
                        println()
                    }
                }
            } ?: println("timeout! search interrupted")
        }
    }

    println("lead time: ${totalTime / 1000.0} seconds")
}