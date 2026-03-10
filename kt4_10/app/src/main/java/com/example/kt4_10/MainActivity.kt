package com.example.kt4_10

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val locationHelper by lazy { LocationHelper(this) }
    private val geocoderHelper by lazy { GeocoderHelper(this) }

    private val _isLoading = mutableStateOf(false)
    private val _address = mutableStateOf<String?>(null)
    private val _coordinates = mutableStateOf<String?>(null)
    private val _error = mutableStateOf<String?>(null)
    private val _hasPermission = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        _hasPermission.value = fineGranted || coarseGranted

        if (_hasPermission.value) {
            getLocation()
        } else {
            _error.value = "Разрешение на геолокацию не предоставлено"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(
                isLoading = _isLoading.value,
                address = _address.value,
                coordinates = _coordinates.value,
                error = _error.value,
                hasPermission = _hasPermission.value,
                onGetAddress = { checkPermissionAndGetAddress() }
            )
        }
    }

    private fun checkPermissionAndGetAddress() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        _hasPermission.value = fineGranted || coarseGranted

        if (_hasPermission.value) {
            getLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getLocation() {
        lifecycleScope.launch {
            _isLoading.value = true
            _error.value = null
            _address.value = null
            _coordinates.value = null

            val locationResult = locationHelper.getCurrentLocation()

            when (locationResult) {
                is LocationResult.Success -> {
                    val lat = locationResult.latitude
                    val lng = locationResult.longitude

                    _coordinates.value = String.format("%.6f, %.6f", lat, lng)

                    val geocodeResult = geocoderHelper.getAddressFromLocation(lat, lng)

                    when (geocodeResult) {
                        is GeocodeResult.Success -> {
                            _address.value = geocodeResult.address
                        }
                        is GeocodeResult.Error -> {
                            _error.value = geocodeResult.message
                        }
                    }
                }
                is LocationResult.Error -> {
                    _error.value = locationResult.message
                }
            }

            _isLoading.value = false
        }
    }
}

@Composable
fun MainScreen(
    isLoading: Boolean,
    address: String?,
    coordinates: String?,
    error: String?,
    hasPermission: Boolean,
    onGetAddress: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Мой адрес",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Индикатор загрузки
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }

        // Адрес (крупным шрифтом)
        if (!address.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = address,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Координаты
        if (!coordinates.isNullOrBlank()) {
            Text(
                text = coordinates,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Ошибка
        if (!error.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопка
        Button(
            onClick = onGetAddress,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                if (isLoading) "Получение..." else "Получить мой адрес"
            )
        }
    }
}