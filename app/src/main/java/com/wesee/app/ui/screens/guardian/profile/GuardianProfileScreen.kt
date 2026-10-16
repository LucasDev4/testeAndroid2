package com.wesee.app.ui.screens.guardian.profile

import androidx.compose.runtime.Composable
import com.wesee.app.data.UserStore
import com.wesee.app.data.acompanhadoName
import com.wesee.app.ui.screens.guardian.components.GuardianBottomBar
import com.wesee.app.ui.screens.guardian.components.GuardianTab
import com.wesee.app.ui.screens.profile.ProfileContent

/**
 * Perfil do Responsável. Nome e inicial vêm da sessão; "Responsável por" = nome do responsável + " Jr.".
 * Localização textual demonstrativa (sem GPS). Só foto e Logout têm ação.
 */
@Composable
fun GuardianProfileScreen(
    userStore: UserStore,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMaps: () -> Unit,
    onLogout: () -> Unit,
) {
    ProfileContent(
        userStore = userStore,
        locationText = "São Paulo - SP, Brasil",
        relationText = "Responsável por: ${acompanhadoName(userStore.name)}",
        onBack = onBack, onLogout = onLogout, showName = false,
    ) { GuardianBottomBar(GuardianTab.Account, onHome = onHome, onMaps = onMaps, onAccount = {}) }
}
