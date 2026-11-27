package com.wesee.app.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wesee.app.R
import com.wesee.app.settings.AppLanguage
import com.wesee.app.settings.AppSettings
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.theme.DarkPalette
import com.wesee.app.ui.theme.LightPalette
import com.wesee.app.ui.theme.WeSeeAccent
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.WeSeeDivider
import com.wesee.app.ui.theme.WeSeeInk
import com.wesee.app.ui.theme.WeSeeMid
import com.wesee.app.ui.theme.WeSeePage
import com.wesee.app.ui.theme.WeSeePalette
import com.wesee.app.ui.theme.WeSeeSurface

/** Páginas abertas pelas opções do Perfil (PCD e Responsável compartilham as mesmas). */
enum class ProfilePage { Appearance, History, Language, Notifications, Terms, Privacy, Faq, DeleteAccount }

/** Mostra a página escolhida por cima do Perfil. O botão voltar do sistema e o do topo fecham a página. */
@Composable
fun ProfilePageHost(page: ProfilePage, onBack: () -> Unit) {
    when (page) {
        ProfilePage.Appearance -> AppearancePage(onBack)
        ProfilePage.History -> HistoryPage(onBack)
        ProfilePage.Language -> LanguagePage(onBack)
        ProfilePage.Notifications -> NotificationsPage(onBack)
        ProfilePage.Terms -> TermsPage(onBack)
        ProfilePage.Privacy -> PrivacyPage(onBack)
        ProfilePage.Faq -> FaqPage(onBack)
        ProfilePage.DeleteAccount -> DeleteAccountPage(onBack)
    }
}

// ------------------------------------------------------------------------------------------------ base

@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    val backLabel = stringResource(R.string.action_back)
    Column(Modifier.fillMaxSize().background(WeSeePage).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(WeSeeBlue)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = backLabel },
                contentAlignment = Alignment.Center,
            ) { Image(painterResource(R.drawable.ic_arrow_left), null, Modifier.width(20.dp)) }
            Spacer(Modifier.width(14.dp))
            WeSeeText(title, WeSeeInk, 26, FontWeight.Bold, Modifier.weight(1f).semantics { heading() }, textAlign = TextAlign.Start)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun Body(text: String) =
    WeSeeText(text, WeSeeMid, 13, modifier = Modifier.fillMaxWidth(), lineHeight = 18, textAlign = TextAlign.Start)

/** Aviso fixo no topo das páginas demonstrativas: deixa claro que o conteúdo é ilustrativo. */
@Composable
private fun DemoNotice(text: String) {
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(WeSeeSurface).border(1.dp, WeSeeAccent, shape).padding(12.dp)) {
        WeSeeText(text, WeSeeInk, 12, FontWeight.SemiBold, lineHeight = 16, textAlign = TextAlign.Start)
    }
}

@Composable
private fun InfoCard(heading: String?, body: String) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().shadow(4.dp, shape).clip(shape).background(WeSeeSurface).padding(16.dp)) {
        if (heading != null) {
            WeSeeText(heading, WeSeeInk, 16, FontWeight.Bold, Modifier.fillMaxWidth().semantics { heading() }, textAlign = TextAlign.Start)
            Spacer(Modifier.height(6.dp))
        }
        WeSeeText(body, WeSeeInk, 13, FontWeight.Normal, Modifier.fillMaxWidth(), lineHeight = 19, textAlign = TextAlign.Start)
    }
}

// ------------------------------------------------------------------------------------------------ escolha única (tema / idioma)

@Composable
private fun RadioMark(selected: Boolean) {
    Box(Modifier.size(24.dp).clip(CircleShape).border(2.dp, if (selected) WeSeeAccent else WeSeeDivider, CircleShape), contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(WeSeeAccent))
    }
}

/** Cartão de escolha única. O estado "selecionado" aparece em borda, marcador E texto (não depende só de cor) e é anunciado pelo TalkBack. */
@Composable
private fun ChoiceCard(
    selected: Boolean, title: String, subtitle: String?, onSelect: () -> Unit, leading: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().shadow(4.dp, shape).clip(shape).background(WeSeeSurface)
            .border(if (selected) 2.dp else 1.dp, if (selected) WeSeeAccent else WeSeeDivider, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            WeSeeText(title, WeSeeInk, 16, FontWeight.Bold, textAlign = TextAlign.Start)
            if (subtitle != null) WeSeeText(subtitle, WeSeeMid, 12, textAlign = TextAlign.Start)
            if (selected) WeSeeText("✓ " + stringResource(R.string.selected_label), WeSeeAccent, 12, FontWeight.Bold, textAlign = TextAlign.Start)
        }
        RadioMark(selected)
    }
}

/** Miniatura do tema, desenhada com a paleta recebida (não depende do tema ativo). */
@Composable
private fun ThemePreview(p: WeSeePalette, description: String) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.size(58.dp, 86.dp).clip(shape).background(p.page).border(1.dp, p.divider, shape).padding(5.dp)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(p.blue))
        Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(4.dp)).background(p.surface))
        Box(Modifier.fillMaxWidth(0.75f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.ink))
        Box(Modifier.fillMaxWidth(0.5f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.mid))
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(3.dp)).background(p.barBg))
    }
}

@Composable
private fun AppearancePage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.appearance_title), onBack) {
    val dark = AppSettings.darkTheme
    Body(stringResource(R.string.appearance_intro))
    val light = stringResource(R.string.theme_light)
    val darkName = stringResource(R.string.theme_dark)
    ChoiceCard(!dark, light, stringResource(R.string.theme_light_desc), { AppSettings.setDarkTheme(false) }) {
        ThemePreview(LightPalette, stringResource(R.string.theme_preview_desc, light))
    }
    ChoiceCard(dark, darkName, stringResource(R.string.theme_dark_desc), { AppSettings.setDarkTheme(true) }) {
        ThemePreview(DarkPalette, stringResource(R.string.theme_preview_desc, darkName))
    }
}

@Composable
private fun LanguagePage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.language_title), onBack) {
    val current = AppSettings.language
    Body(stringResource(R.string.language_intro))
    @Composable fun badge(code: String) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(WeSeeBlue), contentAlignment = Alignment.Center) {
            WeSeeText(code, Color.White, 14, FontWeight.Bold)
        }
    }
    ChoiceCard(current == AppLanguage.PT, stringResource(R.string.language_pt), null, { AppSettings.setLanguage(AppLanguage.PT) }) { badge("PT") }
    ChoiceCard(current == AppLanguage.EN, stringResource(R.string.language_en), null, { AppSettings.setLanguage(AppLanguage.EN) }) { badge("EN") }
    Body(stringResource(R.string.language_voice_note))
}

// ------------------------------------------------------------------------------------------------ notificações

@Composable
private fun SwitchMark(checked: Boolean) {
    val track by animateColorAsState(if (checked) WeSeeAccent else WeSeeDivider, label = "switchTrack")
    val knobX by animateDpAsState(if (checked) 24.dp else 2.dp, label = "switchKnob")
    Box(Modifier.size(54.dp, 32.dp).clip(RoundedCornerShape(50)).background(track), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.offset(x = knobX).size(28.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            WeSeeText(if (checked) "✓" else "–", Color(0xFF243447), 14, FontWeight.Bold)
        }
    }
}

@Composable
private fun NotificationsPage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.notifications_title), onBack) {
    val enabled = AppSettings.notificationsEnabled
    Body(stringResource(R.string.notifications_intro))
    val shape = RoundedCornerShape(18.dp)
    val stateText = stringResource(if (enabled) R.string.notifications_state_on else R.string.notifications_state_off)
    Row(
        Modifier.fillMaxWidth().shadow(4.dp, shape).clip(shape).background(WeSeeSurface)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { AppSettings.setNotificationsEnabled(it) })
            .semantics { stateDescription = stateText }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            WeSeeText(stringResource(R.string.notifications_switch), WeSeeInk, 16, FontWeight.Bold, textAlign = TextAlign.Start)
            WeSeeText(stateText, WeSeeMid, 12, FontWeight.SemiBold, textAlign = TextAlign.Start)
        }
        Spacer(Modifier.width(12.dp))
        SwitchMark(enabled)
    }
    Body(stringResource(R.string.notifications_note))
}

// ------------------------------------------------------------------------------------------------ histórico (demonstrativo)

@Composable
private fun HistoryRow(icon: Int, title: String, description: String, date: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().shadow(4.dp, shape).clip(shape).background(WeSeeSurface).padding(14.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(WeSeeBlue), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            WeSeeText(title, WeSeeInk, 14, FontWeight.Bold, textAlign = TextAlign.Start)
            WeSeeText(description, WeSeeMid, 12, lineHeight = 16, textAlign = TextAlign.Start)
            Spacer(Modifier.height(4.dp))
            WeSeeText(date, WeSeeAccent, 11, FontWeight.SemiBold, textAlign = TextAlign.Start)
        }
    }
}

/** Histórico ILUSTRATIVO: os registros abaixo são fictícios (não vêm de eventos, localização nem usuários reais). */
@Composable
private fun HistoryPage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.history_title), onBack) {
    DemoNotice(stringResource(R.string.history_demo_notice))
    HistoryRow(R.drawable.ic_hist_nav, stringResource(R.string.hist_nav_title), stringResource(R.string.hist_nav_desc), stringResource(R.string.hist_date_1))
    HistoryRow(R.drawable.ic_hist_voice, stringResource(R.string.hist_voice_title), stringResource(R.string.hist_voice_desc), stringResource(R.string.hist_date_2))
    HistoryRow(R.drawable.ic_hist_warning, stringResource(R.string.hist_obstacle_title), stringResource(R.string.hist_obstacle_desc), stringResource(R.string.hist_date_3))
    HistoryRow(R.drawable.ic_hist_chat, stringResource(R.string.hist_chat_title), stringResource(R.string.hist_chat_desc), stringResource(R.string.hist_date_4))
    HistoryRow(R.drawable.ic_hist_pin, stringResource(R.string.hist_dest_title), stringResource(R.string.hist_dest_desc), stringResource(R.string.hist_date_5))
    HistoryRow(R.drawable.ic_hist_tune, stringResource(R.string.hist_settings_title), stringResource(R.string.hist_settings_desc), stringResource(R.string.hist_date_6))
    Spacer(Modifier.height(8.dp))
}

// ------------------------------------------------------------------------------------------------ termos e privacidade (demonstrativos)

@Composable
private fun TermsPage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.terms_title), onBack) {
    DemoNotice(stringResource(R.string.terms_demo_notice))
    InfoCard(stringResource(R.string.terms_s1_h), stringResource(R.string.terms_s1_b))
    InfoCard(stringResource(R.string.terms_s2_h), stringResource(R.string.terms_s2_b))
    InfoCard(stringResource(R.string.terms_s3_h), stringResource(R.string.terms_s3_b))
    InfoCard(stringResource(R.string.terms_s4_h), stringResource(R.string.terms_s4_b))
    InfoCard(stringResource(R.string.terms_s5_h), stringResource(R.string.terms_s5_b))
    InfoCard(stringResource(R.string.terms_s6_h), stringResource(R.string.terms_s6_b))
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PrivacyPage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.privacy_title), onBack) {
    DemoNotice(stringResource(R.string.privacy_demo_notice))
    InfoCard(stringResource(R.string.privacy_s1_h), stringResource(R.string.privacy_s1_b))
    InfoCard(stringResource(R.string.privacy_s2_h), stringResource(R.string.privacy_s2_b))
    InfoCard(stringResource(R.string.privacy_s3_h), stringResource(R.string.privacy_s3_b))
    InfoCard(stringResource(R.string.privacy_s4_h), stringResource(R.string.privacy_s4_b))
    InfoCard(stringResource(R.string.privacy_s5_h), stringResource(R.string.privacy_s5_b))
    InfoCard(stringResource(R.string.privacy_s6_h), stringResource(R.string.privacy_s6_b))
    InfoCard(stringResource(R.string.privacy_s7_h), stringResource(R.string.privacy_s7_b))
    Spacer(Modifier.height(8.dp))
}

// ------------------------------------------------------------------------------------------------ FAQ

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(if (expanded) R.string.faq_state_expanded else R.string.faq_state_collapsed)
    val action = stringResource(if (expanded) R.string.faq_collapse else R.string.faq_expand)
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().shadow(4.dp, shape).clip(shape).background(WeSeeSurface)
            .clickable(role = Role.Button, onClickLabel = action) { expanded = !expanded }
            .semantics { stateDescription = state }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeSeeText(question, WeSeeInk, 14, FontWeight.Bold, Modifier.weight(1f), lineHeight = 18, textAlign = TextAlign.Start)
            Spacer(Modifier.width(10.dp))
            WeSeeText(if (expanded) "−" else "+", WeSeeAccent, 24, FontWeight.Bold)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            WeSeeText(answer, WeSeeMid, 13, Modifier.fillMaxWidth(), lineHeight = 19, textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun FaqPage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.faq_title), onBack) {
    Body(stringResource(R.string.faq_intro))
    FaqItem(stringResource(R.string.faq_q1), stringResource(R.string.faq_a1))
    FaqItem(stringResource(R.string.faq_q2), stringResource(R.string.faq_a2))
    FaqItem(stringResource(R.string.faq_q3), stringResource(R.string.faq_a3))
    FaqItem(stringResource(R.string.faq_q4), stringResource(R.string.faq_a4))
    FaqItem(stringResource(R.string.faq_q5), stringResource(R.string.faq_a5))
    FaqItem(stringResource(R.string.faq_q6), stringResource(R.string.faq_a6))
    FaqItem(stringResource(R.string.faq_q7), stringResource(R.string.faq_a7))
    FaqItem(stringResource(R.string.faq_q8), stringResource(R.string.faq_a8))
    FaqItem(stringResource(R.string.faq_q9), stringResource(R.string.faq_a9))
    Spacer(Modifier.height(8.dp))
}

// ------------------------------------------------------------------------------------------------ apagar conta

/** O backend NÃO tem exclusão de conta: esta página só informa. Nenhum dado é apagado nem simulado como apagado. */
@Composable
private fun DeleteAccountPage(onBack: () -> Unit) = SettingsScaffold(stringResource(R.string.delete_title), onBack) {
    InfoCard(null, stringResource(R.string.delete_info))
    Body(stringResource(R.string.delete_info2))
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(50)).background(WeSeeBlue)
            .clickable(role = Role.Button, onClick = onBack).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { WeSeeText(stringResource(R.string.action_understood), Color.White, 14, FontWeight.Bold) }
}
