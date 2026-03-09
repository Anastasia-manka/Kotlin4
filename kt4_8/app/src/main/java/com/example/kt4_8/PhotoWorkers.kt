package com.example.kt4_8

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


const val KEY_IMAGE_PATH = "image_path"
const val KEY_COMPRESSED_PATH = "compressed_path"
const val KEY_WATERMARK_PATH = "watermark_path"
const val KEY_FINAL_PATH = "final_path"
const val KEY_PROGRESS = "progress"
const val KEY_ERROR = "error"

/**
 * Worker 1: Сжатие фото
 */
class CompressWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WORKER", "Сжатие фото начато")

            // Имитация сжатия
            for (i in 1..100 step 10) {
                if (!coroutineContext.isActive) return Result.failure()
                setProgress(workDataOf(KEY_PROGRESS to i))
                delay(200)
            }


            val compressedPath = "/storage/compressed_${System.currentTimeMillis()}.jpg"

            val resultData = workDataOf(
                KEY_COMPRESSED_PATH to compressedPath
            )

            Log.d("WORKER", "Сжатие завершено: $compressedPath")
            Result.success(resultData)

        } catch (e: Exception) {
            Log.e("WORKER", "Ошибка сжатия", e)
            Result.failure(workDataOf(KEY_ERROR to "Ошибка сжатия: ${e.message}"))
        }
    }
}

/**
 * Worker 2: Добавление водяного знака
 */
class WatermarkWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val compressedPath = inputData.getString(KEY_COMPRESSED_PATH)
                ?: return Result.failure(workDataOf(KEY_ERROR to "Нет пути к сжатому файлу"))

            Log.d("WORKER", "Добавление водяного знака к: $compressedPath")

            for (i in 1..100 step 10) {
                if (!coroutineContext.isActive) return Result.failure()
                setProgress(workDataOf(KEY_PROGRESS to i))
                delay(200)
            }


            val watermarkPath = "/storage/watermark_${System.currentTimeMillis()}.jpg"

            val resultData = workDataOf(
                KEY_WATERMARK_PATH to watermarkPath
            )

            Log.d("WORKER", "Водяной знак добавлен: $watermarkPath")
            Result.success(resultData)

        } catch (e: Exception) {
            Log.e("WORKER", "Ошибка водяного знака", e)
            Result.failure(workDataOf(KEY_ERROR to "Ошибка водяного знака: ${e.message}"))
        }
    }
}

/**
 * Worker 3: Загрузка в облако
 */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val watermarkPath = inputData.getString(KEY_WATERMARK_PATH)
                ?: return Result.failure(workDataOf(KEY_ERROR to "Нет пути к файлу с водяным знаком"))

            Log.d("WORKER", "Загрузка в облако: $watermarkPath")

            for (i in 1..100 step 10) {
                if (!coroutineContext.isActive) return Result.failure()
                setProgress(workDataOf(KEY_PROGRESS to i))
                delay(300)
            }


            val finalPath = watermarkPath // тут был бы URL облака

            val resultData = workDataOf(
                KEY_FINAL_PATH to finalPath
            )

            Log.d("WORKER", "Загрузка завершена: $finalPath")
            Result.success(resultData)

        } catch (e: Exception) {
            Log.e("WORKER", "Ошибка загрузки", e)
            Result.failure(workDataOf(KEY_ERROR to "Ошибка загрузки: ${e.message}"))
        }
    }
}