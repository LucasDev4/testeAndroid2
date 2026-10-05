package com.wesee.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.wesee.app.data.UserStore
import com.wesee.app.ui.screens.disabled.DisabledLoginScreen
import com.wesee.app.ui.screens.disabled.DisabledRegisterScreen
import com.wesee.app.ui.screens.guardian.GuardianLoginScreen
import com.wesee.app.ui.screens.guardian.GuardianRegisterScreen
import com.wesee.app.ui.screens.pcd.chat.PcdChatScreen
import com.wesee.app.ui.screens.pcd.home.PcdHomeScreen
import com.wesee.app.ui.screens.profile.WhoAreYouScreen
import com.wesee.app.ui.screens.welcome.WelcomeScreen
import com.wesee.app.voice.FieldKind

/** PcdHome / PcdChat só são alcançadas pelo fluxo do "PCD visual" (login por voz ou biometria). */
enum class Route {
    Welcome, WhoAreYou, GuardianLogin, GuardianRegister, DisabledLogin, DisabledRegister, PcdHome, PcdChat
}

/** Destino ao pressionar "voltar" (null = sai do app). */
private fun Route.back(): Route? = when (this) {
    Route.Welcome -> null
    Route.WhoAreYou -> Route.Welcome
    Route.GuardianLogin -> Route.WhoAreYou
    Route.GuardianRegister -> Route.GuardianLogin
    Route.DisabledLogin, Route.DisabledRegister -> Route.WhoAreYou
    Route.PcdHome -> null
    Route.PcdChat -> Route.PcdHome
}

@Composable
fun WeSeeApp() {
    val context = LocalContext.current
    val userStore = remember { UserStore(context) }
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
        Route.DisabledRegister -> DisabledRegisterScreen(
            onRegistered = { data ->
                userStore.saveProfile(data[FieldKind.Name].orEmpty(), data[FieldKind.Email].orEmpty())
                route = Route.DisabledLogin
            },
            onBiometricLogin = { route = Route.PcdHome },
        )
        Route.DisabledLogin -> DisabledLoginScreen(onLoginComplete = { _, _ -> route = Route.PcdHome })
        Route.PcdHome -> PcdHomeScreen(firstName = userStore.firstName, onOpenChat = { route = Route.PcdChat })
        Route.PcdChat -> PcdChatScreen(onOpenHome = { route = Route.PcdHome })
    }
}
