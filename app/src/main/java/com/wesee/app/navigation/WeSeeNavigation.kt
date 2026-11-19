package com.wesee.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.wesee.app.data.UserStore
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import com.wesee.app.network.UserType
import com.wesee.app.ui.components.LocalPcdNav
import com.wesee.app.ui.components.PcdNav
import com.wesee.app.ui.screens.disabled.DisabledLoginScreen
import com.wesee.app.ui.screens.disabled.DisabledRegisterScreen
import com.wesee.app.ui.screens.guardian.GuardianLoginScreen
import com.wesee.app.ui.screens.guardian.GuardianRegisterScreen
import com.wesee.app.ui.screens.guardian.home.GuardianHomeScreen
import com.wesee.app.ui.screens.guardian.home.GuardianMapsScreen
import com.wesee.app.ui.screens.guardian.profile.GuardianProfileScreen
import com.wesee.app.ui.screens.pcd.chat.PcdChatScreen
import com.wesee.app.ui.screens.pcd.home.PcdHomeScreen
import com.wesee.app.ui.screens.pcd.maps.PcdMapsScreen
import com.wesee.app.ui.screens.pcd.profile.PcdProfileScreen
import com.wesee.app.ui.screens.profile.WhoAreYouScreen
import com.wesee.app.ui.screens.welcome.WelcomeScreen

enum class Route {
    Welcome, WhoAreYou, GuardianLogin, GuardianRegister, DisabledLogin, DisabledRegister, PcdHome, PcdChat, PcdMaps, PcdProfile,
    GuardianHome, GuardianMaps, GuardianProfile
}

/** O que a pessoa tocou na tela inicial: "Login" ou "Cadastro". Decide para onde "Quem é você?" leva. */
enum class AuthIntent { Login, Register }

/** Pilha de telas: a tela atual é a última; "voltar" desempilha. Sobrevive a rotação/recriação do app. */
private val RouteStackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map { it.name } },
    restore = { saved -> saved.map { Route.valueOf(it) }.toMutableStateList() },
)

@Composable
fun WeSeeApp() {
    val context = LocalContext.current
    val userStore = remember { UserStore(context) }

    // Sessão salva (token do backend) de um usuário PCD → abre direto na Home
    val start = if (userStore.isLoggedIn && userStore.tipo == UserType.PCD_VISUAL) Route.PcdHome else Route.Welcome
    val stack = rememberSaveable(saver = RouteStackSaver) { mutableStateListOf(start) }
    var intent by rememberSaveable { mutableStateOf(AuthIntent.Register) }
    val route = stack.last()

    fun push(r: Route) { stack.add(r) }
    fun replaceTop(r: Route) { stack[stack.lastIndex] = r }
    fun reset(vararg routes: Route) { stack.clear(); stack.addAll(routes) }
    /** Troca de aba (Home/Chat/Maps/Account): a Home fica por baixo, então "voltar" leva à Home. */
    fun goTab(home: Route, target: Route) { reset(home); if (target != home) stack.add(target) }
    fun goBack() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = stack.size > 1) { goBack() }

    // "Acorda" o servidor (planos gratuitos do Render dormem após alguns minutos parados)
    LaunchedEffect(Unit) { AtlasApi.wakeUp() }

    // Valida o token no backend ao abrir o app; se expirou/foi revogado, encerra a sessão.
    LaunchedEffect(Unit) {
        val token = userStore.token ?: return@LaunchedEffect
        when (val r = AtlasApi.me(token)) {
            is ApiResult.Success -> userStore.updateName(r.value.nome)
            is ApiResult.Error -> if (r.code == 401 || r.code == 403) {
                userStore.logout()
                when (stack.last()) {
                    Route.PcdHome, Route.PcdChat, Route.PcdMaps, Route.PcdProfile -> reset(Route.Welcome, Route.WhoAreYou, Route.DisabledLogin)
                    Route.GuardianHome, Route.GuardianMaps, Route.GuardianProfile -> reset(Route.Welcome, Route.WhoAreYou, Route.GuardianLogin)
                    else -> Unit
                }
            }
        }
    }

    val pcdNav = PcdNav(
        onHome = { goTab(Route.PcdHome, Route.PcdHome) }, onChat = { goTab(Route.PcdHome, Route.PcdChat) },
        onMaps = { goTab(Route.PcdHome, Route.PcdMaps) }, onAccount = { goTab(Route.PcdHome, Route.PcdProfile) },
    )
    CompositionLocalProvider(LocalPcdNav provides pcdNav) {
        when (route) {
            // ---- entrada ----
            Route.Welcome -> WelcomeScreen(
                onLogin = { intent = AuthIntent.Login; push(Route.WhoAreYou) },
                onRegister = { intent = AuthIntent.Register; push(Route.WhoAreYou) },
            )
            Route.WhoAreYou -> WhoAreYouScreen(
                onSelectPcd = { push(if (intent == AuthIntent.Login) Route.DisabledLogin else Route.DisabledRegister) },
                onSelectGuardian = { push(if (intent == AuthIntent.Login) Route.GuardianLogin else Route.GuardianRegister) },
            )
            // ---- PCD visual (por voz) ----
            Route.DisabledLogin -> DisabledLoginScreen(
                onLoggedIn = { userStore.saveSession(it); reset(Route.PcdHome) },
                canUseBiometric = userStore.isLoggedIn && userStore.tipo == UserType.PCD_VISUAL,
                onBiometricLogin = { reset(Route.PcdHome) },
            )
            Route.DisabledRegister -> DisabledRegisterScreen(
                onRegistered = { session ->
                    if (session != null) { userStore.saveSession(session); reset(Route.PcdHome) } else { replaceTop(Route.DisabledLogin) }
                },
                canUseBiometric = userStore.isLoggedIn,
                onBiometricLogin = { reset(Route.PcdHome) },
            )
            // ---- Responsável ----
            Route.GuardianLogin -> GuardianLoginScreen(
                onLoggedIn = { userStore.saveSession(it); reset(Route.GuardianHome) },
                onCreateAccount = { replaceTop(Route.GuardianRegister) },
            )
            Route.GuardianRegister -> GuardianRegisterScreen(
                onRegistered = { replaceTop(Route.GuardianLogin) },
                onGoToLogin = { replaceTop(Route.GuardianLogin) },
            )
            // ---- área do PCD ----
            Route.PcdHome -> PcdHomeScreen(firstName = userStore.firstName, onOpenChat = { goTab(Route.PcdHome, Route.PcdChat) })
            Route.PcdChat -> PcdChatScreen(onOpenHome = { goTab(Route.PcdHome, Route.PcdHome) })
            Route.PcdMaps -> PcdMapsScreen()
            Route.PcdProfile -> PcdProfileScreen(
                userStore = userStore,
                onBack = { goTab(Route.PcdHome, Route.PcdHome) },
                onLogout = { userStore.logout(); reset(Route.Welcome, Route.WhoAreYou, Route.DisabledLogin) },
            )
            // ---- área do Responsável ----
            Route.GuardianHome -> GuardianHomeScreen(
                responsavelName = userStore.name,
                onOpenMaps = { goTab(Route.GuardianHome, Route.GuardianMaps) },
                onAccount = { goTab(Route.GuardianHome, Route.GuardianProfile) },
            )
            Route.GuardianMaps -> GuardianMapsScreen(
                responsavelName = userStore.name,
                onOpenHome = { goTab(Route.GuardianHome, Route.GuardianHome) },
                onAccount = { goTab(Route.GuardianHome, Route.GuardianProfile) },
            )
            Route.GuardianProfile -> GuardianProfileScreen(
                userStore = userStore,
                onBack = { goTab(Route.GuardianHome, Route.GuardianHome) },
                onHome = { goTab(Route.GuardianHome, Route.GuardianHome) },
                onMaps = { goTab(Route.GuardianHome, Route.GuardianMaps) },
                onLogout = { userStore.logout(); reset(Route.Welcome, Route.WhoAreYou, Route.GuardianLogin) },
            )
        }
    }
}
