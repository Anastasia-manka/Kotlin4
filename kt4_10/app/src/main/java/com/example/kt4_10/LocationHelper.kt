package com.example.kt4_10

import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data class Error(val message: String) : LocationResult()
}

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Получение текущей локации с использованием современного CurrentLocationRequest
     */
    suspend fun getCurrentLocation(): LocationResult {
        return try {
            // Создаем запрос на получение локации
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(5000)
                .build()

            val cancellationTokenSource = CancellationTokenSource()

            val location = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    request,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        continuation.resumeWithException(Exception("Локация не получена"))
                    }
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }

            LocationResult.Success(location.latitude, location.longitude)

        } catch (e: SecurityException) {
            Log.e("LocationHelper", "Нет разрешений", e)
            LocationResult.Error("Нет разрешений на геолокацию")
        } catch (e: Exception) {
            Log.e("LocationHelper", "Ошибка получения локации", e)
            LocationResult.Error("Ошибка: ${e.message}")
        }
    }


    suspend fun getLastLocation(): LocationResult {
        return try {
            val location = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }

                continuation.invokeOnCancellation {
                    //заглушка
                }
            }

            if (location != null) {
                LocationResult.Success(location.latitude, location.longitude)
            } else {
                LocationResult.Error("Нет сохраненной локации")
            }
        } catch (e: SecurityException) {
            LocationResult.Error("Нет разрешений на геолокацию")
        } catch (e: Exception) {
            LocationResult.Error("Ошибка: ${e.message}")
        }
    }
}