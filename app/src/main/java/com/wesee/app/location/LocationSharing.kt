package com.wesee.app.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.wesee.app.data.UserStore
import com.wesee.app.device.rememberIsForeground
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import kotlinx.coroutines.delay

/**
 * PCD: envia a própria posição ao backend (POST /api/pcd/localizacao) para o responsável vinculado ver.
 * - Só roda com permissão de localização já concedida (quem pede é a Home/Mapas) e com o app VISÍVEL: não há
 *   localização em segundo plano. Fora do app, o responsável vê a última posição enviada, com a hora.
 * - Envia a cada 60 s (e logo que a 1ª posição chega). O backend só GUARDA se houver responsável com permissão de
 *   localização; se responder "não guardado", o app espera 5 min antes de tentar de novo.
 * - Não emite nada na tela.
 */
@Composable
fun PcdLocationSharing(userStore: UserStore) {
    val access = rememberLocationAccess(askOnStart = false)
    val foreground by rememberIsForeground()
    val active = access.hasPermission && foreground
    val fix by rememberUserFix(active, intervalMs = 15_000L, minDistanceMeters = 10f)
    val latest by rememberUpdatedState(fix)

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (true) {
            val current = latest
            val token = userStore.token
            var wait = 5_000L                       // ainda sem posição: tenta de novo logo
            if (current != null && token != null) {
                val r = AtlasApi.publishLocation(token, current.point, current.accuracyMeters)
                wait = if (r is ApiResult.Success && !r.value) 300_000L else 60_000L
            }
            delay(wait)
        }
    }
}
