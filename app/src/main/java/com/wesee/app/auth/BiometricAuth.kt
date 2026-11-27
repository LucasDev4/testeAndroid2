package com.wesee.app.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.wesee.app.R
import com.wesee.app.settings.tr

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Autentica com a biometria do aparelho, se disponível. */
fun authenticateWithBiometrics(context: Context, onSuccess: () -> Unit, onUnavailable: (String) -> Unit) {
    val activity = context.findActivity() ?: return onUnavailable(tr(R.string.bio_unavailable))
    val allowed = BIOMETRIC_STRONG or BIOMETRIC_WEAK
    if (BiometricManager.from(activity).canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
        return onUnavailable(tr(R.string.bio_unavailable_enroll))
    }
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(tr(R.string.voice_biometric_login))
            .setAllowedAuthenticators(allowed)
            .setNegativeButtonText(tr(R.string.action_cancel))
            .build(),
    )
}
