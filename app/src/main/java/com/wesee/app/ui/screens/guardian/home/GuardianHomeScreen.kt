package com.wesee.app.ui.screens.guardian.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wesee.app.data.GuardianDemoData
import com.wesee.app.data.firstNameOf
import com.wesee.app.ui.components.overlapTop
import com.wesee.app.ui.screens.guardian.components.*
import com.wesee.app.ui.theme.WeSeeCard

/**
 * Home do Responsável (dados demonstrativos). Estrutura:
 *  1) topo em destaque (saudação, quem é acompanhado, sino, dispositivo e bateria);
 *  2) localização (endereço, horário e mapa; toque abre a aba Maps);
 *  3) alertas recentes (carrossel);
 *  4) status do dispositivo (4 cartões).
 * O sino abre o painel de notificações (voltar do Android fecha).
 */
@Composable
fun GuardianHomeScreen(responsavelName: String, onOpenMaps: () -> Unit = {}, onAccount: (() -> Unit)? = null) {
    val data = remember(responsavelName) { GuardianDemoData.build(responsavelName) }
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = panelOpen) { panelOpen = false }

    Box(Modifier.fillMaxSize().background(WeSeeCard)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                HomeHero(
                    greetingName = firstNameOf(responsavelName),
                    pessoaNome = data.pessoa.nome,
                    hasUnread = data.notificacoes.any { !it.lida },
                    deviceConnected = data.dispositivo.status == "Conectado",
                    batteryPercent = data.dispositivo.bateria,
                    onBellClick = { panelOpen = !panelOpen },
                )
                // o conteúdo "sobe" 44dp sobre o topo azul (efeito de cartão flutuante)
                Column(Modifier.overlapTop(44.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    LocationHomeCard(data.pessoa, onOpenMaps)
                    Column {
                        AlertsTitle()
                        AlertsCarousel(data.alertas)
                    }
                    StatusSection(data.dispositivo, data.pessoa.nome)
                    Spacer(Modifier.height(4.dp))
                }
            }
            GuardianBottomBar(GuardianTab.Home, onHome = {}, onMaps = onOpenMaps, onAccount = onAccount)
        }
        NotificationsPanelOverlay(panelOpen, data.notificacoes) { panelOpen = false }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable private fun GuardianHomePreview() = GuardianHomeScreen("Lucas")
