package com.wesee.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.wesee.app.network.AddressInfo
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import kotlinx.coroutines.delay

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** O serviço de localização (GPS) do aparelho está ligado? */
fun Context.isLocationServiceEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return LocationManagerCompat.isLocationEnabled(manager)
}

/** Distância em metros entre dois pontos. */
fun distanceMeters(a: LatLng, b: LatLng): Float {
    val out = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
    return out[0]
}

/** Posição do aparelho + precisão estimada (null se o aparelho não informar). */
data class LocationFix(val point: LatLng, val accuracyMeters: Float?)

/**
 * Posição real do aparelho (Fused Location Provider), atualizada SÓ enquanto o composable estiver na tela
 * (nada em segundo plano). Use [enabled] = permissão concedida. [intervalMs]/[minDistanceMeters] controlam o consumo de bateria.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberUserFix(enabled: Boolean, intervalMs: Long = 5000L, minDistanceMeters: Float = 5f): State<LocationFix?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<LocationFix?>(null) }
    DisposableEffect(enabled, intervalMs, minDistanceMeters) {
        if (!enabled) return@DisposableEffect onDispose { }
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { l ->
            if (l != null && state.value == null) {
                state.value = LocationFix(LatLng(l.latitude, l.longitude), if (l.hasAccuracy()) l.accuracy else null)
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistanceMeters).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    state.value = LocationFix(LatLng(it.latitude, it.longitude), if (it.hasAccuracy()) it.accuracy else null)
                }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        onDispose { client.removeLocationUpdates(callback) }
    }
    return state
}

/** Só a posição (LatLng). Mantida para as telas que não precisam da precisão. */
@Composable
fun rememberUserLocation(enabled: Boolean): State<LatLng?> {
    val fix = rememberUserFix(enabled)
    return remember(fix) { derivedStateOf { fix.value?.point } }
}

/** Situação do acesso à localização + ações para o usuário resolver (pedir permissão / abrir ajustes do GPS). */
class LocationAccess(
    val hasPermission: Boolean, val gpsEnabled: Boolean,
    val requestPermission: () -> Unit, val openGpsSettings: () -> Unit,
)

/**
 * Cuida da permissão de localização e do estado do GPS. Enquanto faltar permissão ou o GPS estiver desligado, confere a cada
 * 3 s se o usuário resolveu (pouco custo). [askOnStart] pede a permissão ao abrir a tela. Nunca pede localização em segundo plano.
 */
@Composable
fun rememberLocationAccess(askOnStart: Boolean = true): LocationAccess {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var gpsEnabled by remember { mutableStateOf(context.isLocationServiceEnabled()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = context.hasLocationPermission()
    }
    val requestPermission: () -> Unit = {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) { if (askOnStart && !hasPermission) requestPermission() }
    LaunchedEffect(hasPermission, gpsEnabled) {
        while (!hasPermission || !gpsEnabled) {
            delay(3000)
            hasPermission = context.hasLocationPermission()
            gpsEnabled = context.isLocationServiceEnabled()
        }
    }
    val openGpsSettings: () -> Unit = {
        try {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { /* aparelho sem a tela de ajustes: nada a fazer */ }
    }
    return LocationAccess(hasPermission, gpsEnabled, requestPermission, openGpsSettings)
}

/** Endereço aproximado atual. [failed] = a última consulta falhou (o endereço antigo, se houver, é mantido). */
data class AddressState(val address: AddressInfo?, val failed: Boolean)

/**
 * Endereço aproximado de [point] via backend (geocodificação reversa). Só consulta de novo quando a pessoa se moveu
 * ao menos [minMeters]: poupa bateria, rede e o limite de uso do Nominatim.
 */
@Composable
fun rememberAddress(point: LatLng?, token: String?, minMeters: Float = 50f): State<AddressState> {
    val state = remember { mutableStateOf(AddressState(null, false)) }
    var last by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(point) {
        val p = point ?: return@LaunchedEffect
        val t = token ?: return@LaunchedEffect
        val previous = last
        if (previous != null && distanceMeters(previous, p) < minMeters) return@LaunchedEffect
        when (val r = AtlasApi.reverseGeocode(t, p)) {
            is ApiResult.Success -> { state.value = AddressState(r.value, false); last = p }
            is ApiResult.Error -> state.value = state.value.copy(failed = true)
        }
    }
    return state
}
