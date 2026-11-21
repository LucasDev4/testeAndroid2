package com.wesee.app.ui.screens.pcd.home.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wesee.app.R
import com.wesee.app.data.UserStore
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.voice.VoiceAssistant
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/**
 * Voz + controlador da IA, criados UMA vez pela Home. Assim a câmera pode passar do card para a tela cheia (e voltar)
 * sem repetir a saudação nem perder o estado da detecção.
 */
class CameraSession(val assistant: VoiceAssistant, val controller: SceneDetectionController)

@Composable
fun rememberCameraSession(): CameraSession {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember {
        val assistant = VoiceAssistant(context)
        CameraSession(assistant, SceneDetectionController(scope, UserStore(context), SceneNarrator(assistant)))
    }
    DisposableEffect(session) { onDispose { session.assistant.release() } }
    return session
}

/**
 * Câmera real com IA que FALA o que vê. No card da Home: tocar na câmera mostra o botão "Tela cheia" ([onExpand]).
 * Em [fullscreen]: mostra o botão "Sair da tela cheia" ([onExit]).
 * [onActiveChange] avisa a Home quando a IA está funcionando ("Sistema ativo").
 */
@Composable
fun LiveCameraContent(
    session: CameraSession,
    fullscreen: Boolean = false,
    onExpand: () -> Unit = {},
    onExit: () -> Unit = {},
    onActiveChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    if (hasPermission) {
        CameraWithDetection(session, fullscreen, onExpand, onExit, onActiveChange)
    } else {
        LaunchedEffect(Unit) { onActiveChange(false) }
        Column(
            Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.ic_camera), null, Modifier.size(42.dp))
            Spacer(Modifier.height(10.dp))
            WeSeeText("Permita o uso da câmera para o WeSee descrever o que está à sua frente.", WeSeeBlue, 12, FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50)).background(WeSeeBlue)
                    .clickable(role = Role.Button) { launcher.launch(Manifest.permission.CAMERA) }.padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) { WeSeeText("Permitir câmera", Color.White, 13, FontWeight.Bold) }
        }
    }
}

@Composable
private fun PillButton(label: String, icon: Int, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50)).background(Color(0xCC00172B))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        WeSeeText(label, Color.White, 12, FontWeight.Bold)
    }
}

@Composable
private fun CameraWithDetection(
    session: CameraSession, fullscreen: Boolean, onExpand: () -> Unit, onExit: () -> Unit, onActiveChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = session.controller

    val currentOnActive by rememberUpdatedState(onActiveChange)
    LaunchedEffect(controller.status) { currentOnActive(controller.status == CameraStatus.Active) }
    DisposableEffect(Unit) { onDispose { currentOnActive(false) } }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    // COMPATIBLE (TextureView): respeita os cantos arredondados do card
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            keepScreenOn = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            try {
                val p = providerFuture.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image -> controller.onFrame(image) }
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                controller.cameraReady()
            } catch (e: Exception) {
                controller.cameraFailed()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    // no card: tocar mostra o botão "Tela cheia" por alguns segundos
    var controlsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(controlsVisible) { if (controlsVisible) { delay(6000); controlsVisible = false } }

    val insets = if (fullscreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier

    Box(Modifier.fillMaxSize().semantics { contentDescription = "Câmera do WeSee. A IA descreve por voz o que está à sua frente." }) {
        AndroidView({ previewView }, Modifier.fillMaxSize())

        if (!fullscreen) {
            Box(
                Modifier.matchParentSize().clickable(onClickLabel = "Mostrar o botão de tela cheia") { controlsVisible = !controlsVisible },
            )
        }

        controller.statusMessage?.let { message ->
            Box(Modifier.align(Alignment.TopStart).then(insets).padding(10.dp)) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xCC00172B)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                    WeSeeText(message, Color.White, 10, FontWeight.SemiBold)
                }
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xB300172B))) {
            Box(Modifier.then(if (fullscreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier).padding(horizontal = 14.dp, vertical = 10.dp)) {
                WeSeeText(
                    controller.caption ?: "Aponte a câmera para o caminho à sua frente.",
                    Color.White, if (fullscreen) 15 else 12, FontWeight.SemiBold, Modifier.fillMaxWidth(),
                )
            }
        }

        if (fullscreen) {
            PillButton(
                "Sair da tela cheia", R.drawable.ic_fullscreen_exit, "Sair do modo de tela cheia da câmera", onExit,
                Modifier.align(Alignment.TopEnd).then(insets).padding(10.dp),
            )
        } else if (controlsVisible) {
            PillButton(
                "Tela cheia", R.drawable.ic_fullscreen, "Expandir a câmera para tela cheia", onExpand,
                Modifier.align(Alignment.TopEnd).padding(10.dp),
            )
        }
    }
}

/** Esconde as barras do sistema enquanto a câmera está em tela cheia (volta ao sair). */
@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Camada de tela cheia (com o celular em pé) sobre a Home. Voltar do Android também sai. */
@Composable
fun FullscreenCamera(session: CameraSession, onExit: () -> Unit, onActiveChange: (Boolean) -> Unit = {}) {
    BackHandler(onBack = onExit)
    ImmersiveMode()
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }, // não deixa tocar na Home por baixo
    ) {
        LiveCameraContent(session, fullscreen = true, onExit = onExit, onActiveChange = onActiveChange)
    }
}

/** Mostrado no card da Home enquanto a câmera está em tela cheia. */
@Composable
fun FullscreenPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WeSeeText("Câmera em tela cheia", WeSeeBlue, 13, FontWeight.SemiBold)
    }
}
