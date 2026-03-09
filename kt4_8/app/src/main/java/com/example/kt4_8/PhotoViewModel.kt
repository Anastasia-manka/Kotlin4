package com.example.kt4_8

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ProcessingState {
    object Idle : ProcessingState()
    object Compressing : ProcessingState()
    object Watermarking : ProcessingState()
    object Uploading : ProcessingState()
    data class Success(val message: String) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private var compressWorkId: UUID? = null
    private var watermarkWorkId: UUID? = null
    private var uploadWorkId: UUID? = null

    fun startProcessing() {

        _processingState.value = ProcessingState.Idle
        _progress.value = 0


        val compressWork = OneTimeWorkRequestBuilder<CompressWorker>()
            .setConstraints(constraints)
            .build()

        val watermarkWork = OneTimeWorkRequestBuilder<WatermarkWorker>()
            .setConstraints(constraints)
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .build()


        compressWorkId = compressWork.id
        watermarkWorkId = watermarkWork.id
        uploadWorkId = uploadWork.id


        val continuation = workManager
            .beginWith(compressWork)
            .then(watermarkWork)
            .then(uploadWork)

        continuation.enqueue()


        observeWorkStatus()
    }

    private fun observeWorkStatus() {

        compressWorkId?.let { id ->
            viewModelScope.launch {
                workManager.getWorkInfoByIdFlow(id).collect { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            _processingState.value = ProcessingState.Compressing
                            _progress.value = workInfo.progress.getInt(KEY_PROGRESS, 0)
                            Log.d("VIEWMODEL", "Compressing: ${_progress.value}%")
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(KEY_ERROR) ?: "Ошибка сжатия"
                            _processingState.value = ProcessingState.Error(error)
                            Log.e("VIEWMODEL", "Compress failed: $error")
                        }
                        else -> {}
                    }
                }
            }
        }


        watermarkWorkId?.let { id ->
            viewModelScope.launch {
                workManager.getWorkInfoByIdFlow(id).collect { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            _processingState.value = ProcessingState.Watermarking
                            _progress.value = workInfo.progress.getInt(KEY_PROGRESS, 0)
                            Log.d("VIEWMODEL", "Watermarking: ${_progress.value}%")
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(KEY_ERROR) ?: "Ошибка водяного знака"
                            _processingState.value = ProcessingState.Error(error)
                            Log.e("VIEWMODEL", "Watermark failed: $error")
                        }
                        else -> {}
                    }
                }
            }
        }


        uploadWorkId?.let { id ->
            viewModelScope.launch {
                workManager.getWorkInfoByIdFlow(id).collect { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            _processingState.value = ProcessingState.Uploading
                            _progress.value = workInfo.progress.getInt(KEY_PROGRESS, 0)
                            Log.d("VIEWMODEL", "Uploading: ${_progress.value}%")
                        }
                        WorkInfo.State.SUCCEEDED -> {

                            val finalPath = workInfo.outputData.getString(KEY_FINAL_PATH)
                                ?: "/storage/unknown.jpg"


                            _processingState.value = ProcessingState.Success(
                                "Готово! Фото загружено: $finalPath"
                            )
                            _progress.value = 100
                            Log.d("VIEWMODEL", "Upload succeeded with path: $finalPath")
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(KEY_ERROR) ?: "Ошибка загрузки"
                            _processingState.value = ProcessingState.Error(error)
                            Log.e("VIEWMODEL", "Upload failed: $error")
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun resetState() {
        _processingState.value = ProcessingState.Idle
        _progress.value = 0
    }

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}