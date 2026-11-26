package com.wesee.app.ui.screens.pcd.profile

import androidx.compose.runtime.*
import com.wesee.app.ui.screens.links.LinksController
import com.wesee.app.ui.screens.links.LinksModal
import com.wesee.app.ui.screens.profile.LinksOption
import androidx.compose.ui.platform.LocalContext
import com.wesee.app.data.UserStore
import com.wesee.app.location.hasLocationPermission
import com.wesee.app.location.rememberUserLocation
import com.wesee.app.location.rememberAddress
import com.wesee.app.ui.components.LocalPcdNav
import com.wesee.app.ui.components.PcdBottomBar
import com.wesee.app.ui.components.PcdTab
import com.wesee.app.ui.screens.profile.ProfileContent

/** Perfil do PCD visual (usa a tela compartilhada [ProfileContent]). Apenas foto e Logout têm ação. */
@Composable
fun PcdProfileScreen(userStore: UserStore, onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val nav = LocalPcdNav.current
    val scope = rememberCoroutineScope()
    val links = remember { LinksController(scope, userStore, isGuardian = false) }

    // localização real do usuário (se a permissão já foi concedida; não pede permissão aqui)
    val userLocation = rememberUserLocation(context.hasLocationPermission()).value
    // cidade/estado pelo backend (geocodificação reversa); só consulta de novo se a pessoa andar 200 m ou mais
    val address = rememberAddress(userLocation, userStore.token, minMeters = 200f).value.address
    val place = address?.let { listOfNotNull(it.city, it.state).distinct().joinToString(" - ").ifBlank { it.label.orEmpty() } }
        ?.takeIf { it.isNotBlank() }

    ProfileContent(
        userStore = userStore,
        locationText = place ?: "Localização não disponível",
        relationText = "Responsável não definido ainda...",
        onBack = onBack, onLogout = onLogout,
        linksOption = LinksOption("Meus responsáveis") { links.open() },
        overlay = { LinksModal(links) },
    ) { PcdBottomBar(PcdTab.Account, onHome = nav.onHome, onChat = nav.onChat) }
}
