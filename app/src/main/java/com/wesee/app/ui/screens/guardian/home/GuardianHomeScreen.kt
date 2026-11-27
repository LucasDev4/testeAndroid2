package com.wesee.app.ui.screens.guardian.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wesee.app.data.GuardianDemoData
import com.wesee.app.data.UserStore
import com.wesee.app.data.firstNameOf
import com.wesee.app.ui.screens.guardian.GuardianTracking
import com.wesee.app.ui.screens.guardian.TrackingEffects
import com.wesee.app.ui.screens.guardian.components.*
import com.wesee.app.ui.theme.WeSeeCard
import androidx.compose.ui.res.stringResource
import com.wesee.app.R

/**
 * Home do Responsável. A pessoa acompanhada e a LOCALIZAÇÃO são reais (vínculos + backend); status, alertas e
 * notificações ainda são demonstrativos. O sino abre o painel de notificações.
 */
@Composable
fun GuardianHomeScreen(responsavelName: String, onOpenMaps: () -> Unit = {}, onAccount: (() -> Unit)? = null) {
    val context = LocalContext.current
    val tracking = remember { GuardianTracking(UserStore(context)) }
    TrackingEffects(tracking)
    val pessoa = tracking.pessoa()
    val data = remember(responsavelName, pessoa) { GuardianDemoData.build(responsavelName, pessoa) }
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = panelOpen) { panelOpen = false }

    Box(Modifier.fillMaxSize().background(WeSeeCard)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 14.dp, bottom = 16.dp)) {
                GuardianHeader(greetingName = firstNameOf(responsavelName), pessoaNome = data.pessoa.nome, hasUnread = data.notificacoes.any { !it.lida }, onBellClick = { panelOpen = !panelOpen })
                Spacer(Modifier.height(6.dp))
                DeviceConnectedIndicator()
                if (tracking.pessoas.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    PessoaSelector(tracking.pessoas, tracking.selectedId, onSelect = { tracking.select(it) })
                }
                Spacer(Modifier.height(8.dp))
                LocationSummaryCard(data.pessoa)
                tracking.notice?.let { Spacer(Modifier.height(8.dp)); GuardianNotice(it) }
                Spacer(Modifier.height(18.dp))
                AlertsTitle()
                AlertsCarousel(data.alertas)
                Spacer(Modifier.height(16.dp))
                StatusCard(data.dispositivo, data.pessoa)
                Spacer(Modifier.height(16.dp))
                val lat = data.pessoa.latitude
                val lon = data.pessoa.longitude
                if (lat != null && lon != null) {
                    MapThumbnail(lat, lon, onClick = onOpenMaps)
                } else {
                    GuardianMapPlaceholder(
                        stringResource(if (tracking.loading) R.string.loading else R.string.guardian_no_location_map),
                        Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(110.dp),
                    )
                }
            }
            GuardianBottomBar(GuardianTab.Home, onHome = {}, onMaps = onOpenMaps, onAccount = onAccount)
        }
        NotificationsPanelOverlay(panelOpen, data.notificacoes) { panelOpen = false }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable private fun GuardianHomePreview() = GuardianHomeScreen("Lucas")
