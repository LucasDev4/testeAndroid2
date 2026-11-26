package com.wesee.app.ui.screens.guardian

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.wesee.app.data.PessoaAcompanhada
import com.wesee.app.data.UserStore
import com.wesee.app.device.rememberIsForeground
import com.wesee.app.location.distanceMeters
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import com.wesee.app.network.SharedLocation
import com.wesee.app.network.UserType
import com.wesee.app.network.VinculoItem
import com.wesee.app.network.userMessage
import kotlinx.coroutines.delay

/** "Atualizado há 5 min". A idade vem calculada pelo SERVIDOR (não depende do relógio do celular). */
fun formatUpdatedAge(seconds: Long): String {
    val s = maxOf(0L, seconds)
    return when {
        s < 60 -> "Atualizado há instantes"
        s < 3600 -> "Atualizado há ${s / 60} min"
        s < 86400 -> "Atualizado há ${s / 3600} h"
        else -> "Atualizado há ${s / 86400} dia(s)"
    }
}

/**
 * Estado das telas do Responsável (Home e Localização): pessoas VINCULADAS (GET /api/vinculos), a escolhida e a última
 * localização dela (GET /api/responsavel/pcds/{id}/localizacao). Quem decide se o acesso é permitido é o BACKEND (vínculo ativo
 * + permissão, a cada chamada): o id escolhido aqui é só um pedido, nunca uma autorização.
 * Nada é inventado: sem localização, o estado diz o motivo.
 */
class GuardianTracking(private val userStore: UserStore) {
    var loading by mutableStateOf(true)
    var pessoas by mutableStateOf<List<VinculoItem>>(emptyList())
    var selectedId by mutableStateOf(userStore.selectedPcdId)
    var location by mutableStateOf<SharedLocation?>(null)
    var address by mutableStateOf<String?>(null)
    var addressFailed by mutableStateOf(false)
    var updatedLabel by mutableStateOf("")
    var notice by mutableStateOf<String?>(null)
    var refreshing by mutableStateOf(false)
    private var lastGeocoded: LatLng? = null

    /** Pessoa escolhida; se a salva não existir mais, a primeira da lista. */
    val selected: VinculoItem?
        get() = pessoas.firstOrNull { it.relacionadoId == selectedId } ?: pessoas.firstOrNull()

    suspend fun loadPeople() {
        val token = userStore.token
        if (token == null) {
            loading = false
            notice = "Sua sessão expirou. Entre novamente."
            return
        }
        when (val r = AtlasApi.listarVinculos(token)) {
            is ApiResult.Success -> {
                pessoas = r.value.filter { it.status == "ATIVO" && it.tipo == UserType.PCD_VISUAL }
                val current = selected
                if (current != null) {
                    selectedId = current.relacionadoId
                    userStore.selectedPcdId = current.relacionadoId
                    notice = null
                } else {
                    notice = "Você ainda não tem nenhuma pessoa vinculada. Peça um código no app da pessoa."
                }
            }
            is ApiResult.Error -> notice = r.userMessage()
        }
        loading = false
    }

    fun select(person: VinculoItem) {
        if (person.relacionadoId == selectedId) return
        selectedId = person.relacionadoId
        userStore.selectedPcdId = person.relacionadoId
        location = null
        address = null
        addressFailed = false
        updatedLabel = ""
        notice = null
        lastGeocoded = null
    }

    suspend fun refreshLocation() {
        val person = selected ?: return
        val token = userStore.token ?: return
        refreshing = true
        val r = AtlasApi.pcdLocation(token, person.relacionadoId)
        refreshing = false
        if (selected?.relacionadoId != person.relacionadoId) return   // a escolha mudou enquanto esperava: descarta
        when (r) {
            is ApiResult.Success -> {
                val loc = r.value
                if (loc == null) {
                    clearLocation("O aparelho de ${person.nome} ainda não enviou a localização.")
                } else {
                    location = loc
                    updatedLabel = formatUpdatedAge(loc.ageSeconds)
                    notice = null
                    lookupAddress(token, loc.latLng)
                }
            }
            is ApiResult.Error -> {
                if (r.code == 403 || r.code == 404) clearLocation(r.message)   // sem vínculo/permissão: não mostra nada
                else notice = r.userMessage()                                  // falha de rede: mantém a última posição conhecida
            }
        }
    }

    private fun clearLocation(message: String) {
        location = null
        address = null
        addressFailed = false
        updatedLabel = ""
        lastGeocoded = null
        notice = message
    }

    private suspend fun lookupAddress(token: String, point: LatLng) {
        val last = lastGeocoded
        if (address != null && last != null && distanceMeters(last, point) < 30f) return
        when (val r = AtlasApi.reverseGeocode(token, point)) {
            is ApiResult.Success -> {
                address = r.value.label
                addressFailed = r.value.label == null
                lastGeocoded = point
            }
            is ApiResult.Error -> addressFailed = true
        }
    }

    /** O que os cards do Responsável exibem (os textos dizem o estado real; nada é inventado). */
    fun pessoa(): PessoaAcompanhada {
        val nome = selected?.nome?.takeIf { it.isNotBlank() } ?: "Pessoa acompanhada"
        val l = location
        val endereco = when {
            loading -> "Carregando..."
            selected == null -> "Nenhuma pessoa vinculada"
            l == null -> "Localização indisponível"
            else -> address ?: if (addressFailed) "Endereço indisponível" else "Buscando endereço..."
        }
        return PessoaAcompanhada(nome, endereco, if (l == null) "" else updatedLabel, l?.latLng?.latitude, l?.latLng?.longitude)
    }
}

/** Carrega os vínculos e atualiza a localização a cada 30 s, SÓ com o app visível (pausa em segundo plano). */
@Composable
fun TrackingEffects(tracking: GuardianTracking) {
    val foreground by rememberIsForeground()
    LaunchedEffect(Unit) { tracking.loadPeople() }
    val selectedId = tracking.selected?.relacionadoId
    LaunchedEffect(selectedId, foreground) {
        if (selectedId == null || !foreground) return@LaunchedEffect
        while (true) {
            tracking.refreshLocation()
            delay(30_000L)
        }
    }
}
