package com.wesee.app.maps

import com.wesee.app.BuildConfig
import java.util.Locale
import kotlin.math.roundToInt

/**
 * O app usa o Google Maps SDK SOMENTE para desenhar o mapa (MAPS_API_KEY).
 * Pesquisa de endereços, geocodificação reversa e rotas NÃO são mais feitas com Google Places/Routes: passam pelo
 * backend do WeSee (Nominatim e OSRM). Veja AtlasApi.searchPlaces / reverseGeocode / route.
 */
object MapsApi {
    val isConfigured: Boolean get() = BuildConfig.MAPS_API_KEY.isNotBlank()
}

fun formatDistance(meters: Int): String =
    if (meters < 1000) "$meters m" else String.format(Locale("pt", "BR"), "%.1f km", meters / 1000.0)

fun formatDuration(seconds: Int): String {
    val min = maxOf(1, (seconds / 60.0).roundToInt())
    return if (min < 60) "~$min min" else "~${min / 60} h ${min % 60} min"
}
