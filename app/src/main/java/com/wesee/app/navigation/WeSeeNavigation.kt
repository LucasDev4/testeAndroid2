package com.wesee.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.wesee.app.ui.screens.disabled.DisabledLoginScreen
import com.wesee.app.ui.screens.disabled.DisabledRegisterScreen
import com.wesee.app.ui.screens.guardian.GuardianLoginScreen
import com.wesee.app.ui.screens.guardian.GuardianRegisterScreen
import com.wesee.app.ui.screens.profile.WhoAreYouScreen
import com.wesee.app.ui.screens.welcome.WelcomeScreen

enum class Route { Welcome, WhoAreYou, GuardianLogin, GuardianRegister, DisabledLogin, DisabledRegister }

/** Destino ao pressionar "voltar" (null = sai do app). */
private fun Route.back(): Route? = when (this) {
    Route.Welcome -> null
    Route.WhoAreYou -> Route.Welcome
    Route.GuardianLogin -> Route.WhoAreYou
    Route.GuardianRegister -> Route.GuardianLogin
    Route.DisabledLogin, Route.DisabledRegister -> Route.WhoAreYou
}

@Composable
fun WeSeeApp() {
    var route by rememberSaveable { mutableStateOf(Route.Welcome) }
    BackHandler(enabled = route.back() != null) { route = route.back()!! }

    when (route) {
        Route.Welcome -> WelcomeScreen(onLogin = {}, onRegister = { route = Route.WhoAreYou })
        Route.WhoAreYou -> WhoAreYouScreen(
            onSelectPcd = { route = Route.DisabledRegister },
            onSelectGuardian = { route = Route.GuardianLogin },
        )
        Route.GuardianLogin -> GuardianLoginScreen(onCreateAccount = { route = Route.GuardianRegister })
        Route.GuardianRegister -> GuardianRegisterScreen(onGoToLogin = { route = Route.GuardianLogin })
        Route.DisabledRegister -> DisabledRegisterScreen(onRegistered = { route = Route.DisabledLogin })
        Route.DisabledLogin -> DisabledLoginScreen()
    }
}
