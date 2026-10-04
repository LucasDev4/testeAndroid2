package com.wesee.app.ui.screens.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wesee.app.R
import com.wesee.app.ui.components.*
import com.wesee.app.ui.theme.WeSeeCard
import com.wesee.app.ui.theme.WeSeeSoftText

/** Tela 3 — Login do responsável. */
@Composable
fun GuardianLoginScreen(
    onLogin: (email: String, password: String) -> Unit = { _, _ -> },
    onForgotPassword: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(WeSeeCard).imePadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().statusBarsPadding(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            WeSeeHeader(2)
            Spacer(Modifier.height(40.dp))
            WeSeeEyes(R.drawable.wesee_eyes_guardian)
        }
        WeSeeBottomPanel(Modifier.heightIn(min = 330.dp)) {
            WeSeeText(stringResource(R.string.guardian_login_title), Color.White, 40, FontWeight.SemiBold, textAlign = TextAlign.Start)
            WeSeeText(stringResource(R.string.guardian_login_subtitle), WeSeeSoftText, 12, textAlign = TextAlign.Start)
            Spacer(Modifier.height(16.dp))
            WeSeeTextField(R.drawable.ic_mail, stringResource(R.string.field_email_hint), email, { email = it },
                keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            WeSeeTextField(R.drawable.ic_lock, stringResource(R.string.field_password_hint), password, { password = it },
                isPassword = true)
            Spacer(Modifier.height(6.dp))
            WeSeeText(stringResource(R.string.forgot_password), WeSeeSoftText, 11,
                modifier = Modifier.clickable(onClick = onForgotPassword), textAlign = TextAlign.Start)
            Spacer(Modifier.height(16.dp))
            PanelButton(stringResource(R.string.action_login), onClick = { onLogin(email, password) })
            Spacer(Modifier.height(10.dp))
            AuthFooter(stringResource(R.string.no_account_prefix), stringResource(R.string.create_account), onCreateAccount)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable private fun GuardianLoginPreview() = GuardianLoginScreen()
