package com.example.kt4_10

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

sealed class GeocodeResult {
    data class Success(val address: String) : GeocodeResult()
    data class Error(val message: String) : GeocodeResult()
}

class GeocoderHelper(private val context: Context) {

    /**
     * Преобразование координат в адрес
     * Использует современный асинхронный Geocoder для API 33+
     */
    suspend fun getAddressFromLocation(lat: Double, lng: Double): GeocodeResult {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getAddressesAsync(geocoder, lat, lng)
            } else {
                geocoder.getFromLocation(lat, lng, 1) ?: emptyList()
            }

            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                val fullAddress = formatAddress(address)
                GeocodeResult.Success(fullAddress)
            } else {
                GeocodeResult.Error("Адрес не найден")
            }

        } catch (e: Exception) {
            GeocodeResult.Error("Ошибка геокодирования: ${e.message}")
        }
    }

    /**
     * Асинхронный метод для Android 13+
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddressesAsync(
        geocoder: Geocoder,
        lat: Double,
        lng: Double
    ): List<Address> = suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocation(lat, lng, 1) { addresses ->
            continuation.resume(addresses)
        }

        continuation.invokeOnCancellation {
            //Geocoder не предоставляет механизм отмены, поэтому просто логируем
        }
    }

    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()

        // Собираем адрес по частям
        address.thoroughfare?.let { parts.add(it) }  // Улица
        address.locality?.let { parts.add(it) }      // Город
        address.adminArea?.let { parts.add(it) }     // Область/регион
        address.countryName?.let { parts.add(it) }   // Страна
        address.postalCode?.let { parts.add(it) }    // Индекс

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            // Если не удалось собрать адрес, возвращаем хотя бы что-то
            address.getAddressLine(0) ?: "Адрес не определен"
        }
    }
}