package com.wesee.app.ui.screens.disabled

import androidx.compose.runtime.Composable
import com.wesee.app.voice.FieldKind
import com.wesee.app.voice.VoiceStep

private val loginSteps = listOf(
    VoiceStep(FieldKind.Email, "Qual é o seu e-mail?"),
    VoiceStep(FieldKind.Password, "Agora diga sua senha.", secret = true),
)

/** Tela 5 — Login do usuário com deficiência visual (100% por voz). */
@Composable
fun DisabledLoginScreen(onLoginComplete: (email: String?, password: String?) -> Unit = { _, _ -> }) {
    val host = rememberVoiceHost(loginSteps, "Login por voz.", "Login concluído.") {
        onLoginComplete(it[FieldKind.Email], it[FieldKind.Password])
    }
    VoiceAuthScreen("Login", host) {
        host.session.announce("Login concluído.")
        onLoginComplete(null, null) // autenticado pela biometria
    }
}
