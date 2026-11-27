package com.wesee.app.ui.screens.disabled

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wesee.app.R
import com.wesee.app.auth.authenticateWithBiometrics
import com.wesee.app.ui.components.WeSeeEyes
import com.wesee.app.ui.components.WeSeeHeader
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.WeSeeAccent
import com.wesee.app.ui.theme.WeSeeCard
import com.wesee.app.ui.theme.WeSeeSurface
import com.wesee.app.ui.theme.WeSeeDeep
import com.wesee.app.voice.*
import androidx.compose.ui.res.stringResource
import com.wesee.app.settings.tr
import com.wesee.app.ui.theme.WeSeeInk

/** Mantém assistente + conversa vivos enquanto a tela existe e pede a permissão do microfone. */
class VoiceHost(val assistant: VoiceAssistant, val session: VoiceSession)

@Composable
fun rememberVoiceHost(
    steps: List<VoiceStep>, intro: String, onFinished: (VoiceSession, Map<FieldKind, String>) -> Unit,
): VoiceHost {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onFinished)
    val host = remember {
        val assistant = VoiceAssistant(context)
        VoiceHost(assistant, VoiceSession(assistant, steps, intro) { session, answers -> latestOnFinished(session, answers) })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) host.session.begin()
        else host.session.announce(tr(R.string.voice_mic_permission))
    }
    LaunchedEffect(host) {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (ok) host.session.begin() else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(host) { onDispose { host.assistant.release() } }
    return host
}

/** Botão de biometria: discreto, mas com área de toque de 48dp. */
@Composable
private fun BiometricChip(onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        val shape = RoundedCornerShape(50)
        Box(Modifier.shadow(3.dp, shape).clip(shape).background(WeSeeSurface).padding(horizontal = 14.dp, vertical = 5.dp)) {
            WeSeeText(stringResource(R.string.voice_biometric_login), WeSeeInk, 10, FontWeight.Bold)
        }
    }
}

/** Botão circular do microfone: normal, ouvindo (pulsa) e processando (anel girando). */
@Composable
private fun MicButton(state: VoiceState, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(1f, 1.35f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "spin")
    val description = when (state) {
        VoiceState.Listening -> stringResource(R.string.voice_mic_listening)
        VoiceState.Processing -> stringResource(R.string.voice_mic_processing)
        VoiceState.Speaking -> stringResource(R.string.voice_mic_speaking)
        VoiceState.Idle -> stringResource(R.string.voice_mic_idle)
    }
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        if (state == VoiceState.Listening) {
            Box(Modifier.size(150.dp).scale(pulse).clip(CircleShape).background(WeSeeAccent.copy(alpha = 0.18f)))
        }
        if (state == VoiceState.Processing) {
            Canvas(Modifier.size(172.dp).rotate(spin)) {
                drawArc(WeSeeAccent, 0f, 110f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Box(
            Modifier.size(150.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(WeSeeBlue, WeSeeDeep)))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(R.drawable.ic_mic), null, Modifier.size(44.dp)) }
    }
}

/** Layout das telas por voz: logo, progresso, olhos, microfone, título e biometria. Sem campos de entrada. */
@Composable
fun VoiceAuthContent(title: String, state: VoiceState, onMic: () -> Unit, onBiometric: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(WeSeeCard).statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WeSeeHeader(1)
        Spacer(Modifier.height(32.dp))
        WeSeeEyes(R.drawable.wesee_eyes_guardian)
        Spacer(Modifier.weight(1f))
        MicButton(state, onMic)
        WeSeeText(title, WeSeeInk, 24, FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        BiometricChip(onBiometric)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun VoiceAuthScreen(title: String, host: VoiceHost, onBiometricSuccess: () -> Unit) {
    val context = LocalContext.current
    VoiceAuthContent(
        title = title,
        state = host.session.state,
        onMic = host.session::onMicTap,
        onBiometric = {
            authenticateWithBiometrics(context, onSuccess = onBiometricSuccess, onUnavailable = host.session::announce)
        },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable private fun VoiceAuthPreview() = VoiceAuthContent("Login", VoiceState.Idle, {}, {})
