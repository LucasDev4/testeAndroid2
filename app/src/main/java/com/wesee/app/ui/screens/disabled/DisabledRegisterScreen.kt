package com.wesee.app.ui.screens.disabled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import com.wesee.app.network.AuthSession
import com.wesee.app.network.RegisterBody
import com.wesee.app.network.UserType
import com.wesee.app.network.spokenFailure
import com.wesee.app.voice.FieldKind
import com.wesee.app.voice.VoiceStep
import kotlinx.coroutines.launch
import com.wesee.app.R
import com.wesee.app.settings.tr

private fun registerSteps() = listOf(
    VoiceStep(FieldKind.Name, tr(R.string.voice_q_name)),
    VoiceStep(FieldKind.Email, tr(R.string.voice_q_email2)),
    VoiceStep(FieldKind.Password, tr(R.string.voice_q_password_reg), secret = true),
    VoiceStep(FieldKind.ConfirmPassword, tr(R.string.voice_q_confirm), secret = true),
)

/**
 * Tela 6 — Cadastro do usuário PCD visual (100% por voz) no backend (/auth/register) e login automático.
 * [onRegistered] recebe a sessão, ou null se o cadastro deu certo mas o login automático falhou.
 */
@Composable
fun DisabledRegisterScreen(
    onRegistered: (AuthSession?) -> Unit = {},
    canUseBiometric: Boolean = false,
    onBiometricLogin: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val host = rememberVoiceHost(registerSteps(), tr(R.string.voice_intro_register)) { session, answers ->
        scope.launch {
            val nome = answers[FieldKind.Name].orEmpty()
            val email = answers[FieldKind.Email].orEmpty()
            val senha = answers[FieldKind.Password].orEmpty()
            when (val r = AtlasApi.register(RegisterBody(nome, email, senha, tipo = UserType.PCD_VISUAL))) {
                is ApiResult.Success -> when (val l = AtlasApi.login(email, senha)) {
                    is ApiResult.Success ->
                        session.complete(tr(R.string.voice_register_done, nome.substringBefore(' '))) { onRegistered(l.value) }
                    is ApiResult.Error ->
                        session.complete(tr(R.string.voice_register_done_login)) { onRegistered(null) }
                }
                is ApiResult.Error -> when {
                    r.code == 409 -> session.retryFrom(FieldKind.Email, tr(R.string.voice_email_taken))
                    r.code == 400 -> {
                        val key = when {
                            r.fields.containsKey("senha") -> FieldKind.Password
                            r.fields.containsKey("email") -> FieldKind.Email
                            else -> FieldKind.Name
                        }
                        session.retryFrom(key, r.fields.values.firstOrNull() ?: tr(R.string.voice_invalid_data))
                    }
                    else -> session.failSubmit(r.spokenFailure())
                }
            }
        }
    }
    VoiceAuthScreen(tr(R.string.action_register), host) {
        if (canUseBiometric) { host.session.announce(tr(R.string.voice_login_done_short)); onBiometricLogin() }
        else host.session.announce(tr(R.string.voice_bio_hint))
    }
}
