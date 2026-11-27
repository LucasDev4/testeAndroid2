package com.wesee.app.ui.screens.disabled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import com.wesee.app.network.AuthSession
import com.wesee.app.network.UserType
import com.wesee.app.network.spokenFailure
import com.wesee.app.voice.FieldKind
import com.wesee.app.voice.VoiceStep
import kotlinx.coroutines.launch
import com.wesee.app.R
import com.wesee.app.settings.tr

private fun loginSteps() = listOf(
    VoiceStep(FieldKind.Email, tr(R.string.voice_q_email)),
    VoiceStep(FieldKind.Password, tr(R.string.voice_q_password_login), secret = true),
)

/** Tela 5 — Login do usuário PCD visual (100% por voz) autenticando no backend (/auth/login). */
@Composable
fun DisabledLoginScreen(
    onLoggedIn: (AuthSession) -> Unit = {},
    canUseBiometric: Boolean = false,
    onBiometricLogin: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val host = rememberVoiceHost(loginSteps(), tr(R.string.voice_intro_login)) { session, answers ->
        scope.launch {
            val email = answers[FieldKind.Email].orEmpty()
            when (val r = AtlasApi.login(email, answers[FieldKind.Password].orEmpty())) {
                is ApiResult.Success ->
                    if (r.value.tipo == UserType.PCD_VISUAL)
                        session.complete(tr(R.string.voice_login_done, r.value.nome.substringBefore(' '))) { onLoggedIn(r.value) }
                    else session.retryFrom(FieldKind.Email, tr(R.string.voice_wrong_type_guardian))
                is ApiResult.Error -> when {
                    r.code == 401 || r.code == 404 -> session.retryFrom(FieldKind.Email, tr(R.string.err_bad_credentials))
                    r.code == 400 -> session.retryFrom(FieldKind.Email, r.fields.values.firstOrNull() ?: tr(R.string.voice_invalid_data))
                    else -> session.failSubmit(r.spokenFailure())
                }
            }
        }
    }
    VoiceAuthScreen(tr(R.string.action_login), host) {
        if (canUseBiometric) { host.session.announce(tr(R.string.voice_login_done_short)); onBiometricLogin() }
        else host.session.announce(tr(R.string.voice_bio_hint))
    }
}
