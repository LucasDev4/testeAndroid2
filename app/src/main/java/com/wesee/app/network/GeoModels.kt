package com.wesee.app.network

import com.google.android.gms.maps.model.LatLng

/**
 * Modelos de endereço, rota e localização compartilhada. Refletem os DTOs do backend (/api/geocoding, /api/routes,
 * /api/responsavel/pcds/{id}/localizacao): o app nunca vê o formato original do Nominatim/OSRM.
 * As coordenadas já chegam como (latitude, longitude): a inversão lon/lat do OSRM é feita no backend.
 */

/** Resultado de pesquisa de destino. */
data class GeoPlace(
    val name: String, val displayName: String, val latLng: LatLng,
    val suburb: String?, val city: String?, val state: String?,
) {
    val title: String get() = name.ifBlank { displayName }
    val subtitle: String get() = listOfNotNull(suburb, city, state).joinToString(", ").ifBlank { displayName }
}

data class PlaceSearch(val places: List<GeoPlace>, val attribution: String)

/** Endereço aproximado de uma coordenada (geocodificação reversa). */
data class AddressInfo(val shortAddress: String?, val displayName: String?, val city: String?, val state: String?) {
    /** Texto para exibir: endereço curto ("Rua X, 123 - Bairro") ou, na falta dele, o endereço completo. */
    val label: String? get() = shortAddress ?: displayName
}

/** [instruction] é a tradução da manobra feita pelo backend; null quando o OSRM não deu informação suficiente. */
data class RouteStepInfo(val instruction: String?, val streetName: String?, val distanceMeters: Int, val location: LatLng?)

/**
 * Rota calculada. [walking] = false significa que o servidor NÃO está confirmado como pedestre: a tela deve mostrar
 * [warning]. [safetyNotice] lembra que a rota não garante acessibilidade nem segurança.
 */
data class RouteData(
    val points: List<LatLng>, val distanceMeters: Int, val durationSeconds: Int, val steps: List<RouteStepInfo>,
    val walking: Boolean, val warning: String?, val safetyNotice: String?, val attribution: String?,
)

/** Última localização do PCD, como o responsável a recebe. [ageSeconds] é calculada no servidor. */
data class SharedLocation(val latLng: LatLng, val accuracyMeters: Double?, val updatedAtIso: String?, val ageSeconds: Long)
