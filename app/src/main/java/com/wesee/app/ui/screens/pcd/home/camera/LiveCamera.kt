package com.wesee.app.ui.screens.pcd.home.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.wesee.app.R
import com.wesee.app.data.UserStore
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.voice.VoiceAssistant
import java.util.concurrent.Executors

/**
 * Câmera real do card da Home do PCD: mostra a prévia, envia quadros para a IA (via backend) e FALA o que está vendo.
 * [onActiveChange] avisa a Home quando a IA está funcionando (para trocar "Sistema desativado" por "Sistema ativo").
 */
@Composable
fun LiveCameraContent(onActiveChange: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    if (hasPermission) {
        CameraWithDetection(onActiveChange)
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
private fun CameraWithDetection(onActiveChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val userStore = remember { UserStore(context) }

    val assistant = remember { VoiceAssistant(context) }
    DisposableEffect(Unit) { onDispose { assistant.release() } }
    val narrator = remember { SceneNarrator(assistant) }
    val controller = remember { SceneDetectionController(scope, userStore, narrator) }

    val currentOnActive by rememberUpdatedState(onActiveChange)
    LaunchedEffect(controller.status) { currentOnActive(controller.status == CameraStatus.Active) }
    DisposableEffect(Unit) { onDispose { currentOnActive(false) } }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    // COMPATIBLE (TextureView): respeita cantos arredondados do card, diferente do SurfaceView
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

    Box(Modifier.fillMaxSize().semantics { contentDescription = "Câmera do WeSee. A IA descreve por voz o que está à sua frente." }) {
        AndroidView({ previewView }, Modifier.fillMaxSize())

        controller.statusMessage?.let { message ->
            Box(
                Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(50))
                    .background(Color(0xCC00172B)).padding(horizontal = 12.dp, vertical = 5.dp),
            ) { WeSeeText(message, Color.White, 10, FontWeight.SemiBold) }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xB300172B)).padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            WeSeeText(
                controller.caption ?: "Aponte a câmera para o caminho à sua frente.",
                Color.White, 12, FontWeight.SemiBold, Modifier.fillMaxWidth(),
            )
        }
    }
}
