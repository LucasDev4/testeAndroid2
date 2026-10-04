package com.wesee.app.ui.screens.disabled

import androidx.compose.runtime.Composable
import com.wesee.app.voice.FieldKind
import com.wesee.app.voice.VoiceStep

private val registerSteps = listOf(
    VoiceStep(FieldKind.Name, "Qual é o seu nome?"),
    VoiceStep(FieldKind.Email, "Agora diga o seu e-mail."),
    VoiceStep(FieldKind.Password, "Agora diga a senha que você deseja utilizar.", secret = true),
    VoiceStep(FieldKind.ConfirmPassword, "Para confirmar, diga novamente a sua senha.", secret = true),
)

/** Tela 6 — Cadastro do usuário com deficiência visual (100% por voz). */
@Composable
fun DisabledRegisterScreen(onRegistered: (Map<FieldKind, String>) -> Unit = {}, onBiometricLogin: () -> Unit = {}) {
    val host = rememberVoiceHost(registerSteps, "Cadastro por voz.", "Cadastro concluído.", onRegistered)
    VoiceAuthScreen("Cadastro", host) {
        host.session.announce("Login concluído.")
        onBiometricLogin()
    }
}
