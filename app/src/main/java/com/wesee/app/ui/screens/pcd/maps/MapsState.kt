package com.wesee.app.ui.screens.pcd.maps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import com.wesee.app.network.GeoPlace
import com.wesee.app.network.RouteData
import com.wesee.app.network.userMessage
import com.wesee.app.R
import com.wesee.app.settings.tr

/**
 * Estado e lógica da tela de Mapas do PCD (pesquisa de destino + rota), fora do composable.
 * Tudo passa pelo backend (AtlasApi). Cada busca/rota recebe um número de "geração": se uma resposta antiga chegar
 * depois de uma nova solicitação (ou de um "limpar"), ela é DESCARTADA — o mapa nunca mostra uma rota velha como se fosse a nova.
 */
class MapsState(private val tokenProvider: () -> String?) {
    var query by mutableStateOf("")
    var searching by mutableStateOf(false)
    var results by mutableStateOf<List<GeoPlace>>(emptyList())
    var searchMessage by mutableStateOf<String?>(null)
    var destination by mutableStateOf<GeoPlace?>(null)
    var route by mutableStateOf<RouteData?>(null)
    var routing by mutableStateOf(false)
    var routeError by mutableStateOf<String?>(null)

    private var searchGeneration = 0
    private var routeGeneration = 0

    /** Pesquisa o texto digitado. Só é chamada quando o usuário confirma (teclado "buscar" ou botão), nunca a cada tecla. */
    suspend fun search(near: LatLng?) {
        val text = query.trim()
        if (text.length < 3) {
            results = emptyList()
            searchMessage = tr(R.string.maps_search_too_short)
            return
        }
        val token = tokenProvider()
        if (token == null) {
            searchMessage = tr(R.string.session_expired_spoken)
            return
        }
        val generation = ++searchGeneration
        searching = true
        searchMessage = null
        results = emptyList()
        val r = AtlasApi.searchPlaces(token, text, near)
        if (generation != searchGeneration) return
        searching = false
        when (r) {
            is ApiResult.Success -> {
                results = r.value.places
                if (r.value.places.isEmpty()) searchMessage = tr(R.string.maps_no_results, text)
            }
            is ApiResult.Error -> searchMessage = r.userMessage()
        }
    }

    /** Escolhe um resultado como destino (a rota é calculada pela tela, quando houver posição atual). */
    fun select(place: GeoPlace) {
        routeGeneration++
        destination = place
        query = place.title
        results = emptyList()
        searchMessage = null
        route = null
        routeError = null
        routing = false
    }

    /** Calcula a rota da posição atual até o destino. Limpa a rota anterior antes de pedir a nova. */
    suspend fun calculateRoute(origin: LatLng) {
        val dest = destination ?: return
        val token = tokenProvider()
        if (token == null) {
            route = null
            routeError = tr(R.string.session_expired_spoken)
            return
        }
        val generation = ++routeGeneration
        route = null
        routeError = null
        routing = true
        val r = AtlasApi.route(token, origin, dest.latLng)
        if (generation != routeGeneration) return
        routing = false
        when (r) {
            is ApiResult.Success -> route = r.value
            is ApiResult.Error -> {
                route = null
                routeError = r.userMessage()
            }
        }
    }

    fun clear() {
        searchGeneration++
        routeGeneration++
        query = ""
        results = emptyList()
        searchMessage = null
        searching = false
        destination = null
        route = null
        routing = false
        routeError = null
    }
}
