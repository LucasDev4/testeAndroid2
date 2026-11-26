package com.wesee.app.ui.screens.pcd.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wesee.app.data.NotesStore
import com.wesee.app.data.UserStore
import com.wesee.app.location.rememberAddress
import com.wesee.app.location.rememberLocationAccess
import com.wesee.app.location.rememberUserFix
import kotlin.math.roundToInt
import com.wesee.app.device.rememberBatteryState
import com.wesee.app.ui.components.PcdBottomBar
import com.wesee.app.ui.components.PcdTab
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.components.overlapTop
import com.wesee.app.ui.screens.pcd.home.camera.FullscreenCamera
import com.wesee.app.ui.screens.pcd.home.camera.FullscreenPlaceholder
import com.wesee.app.ui.screens.pcd.home.camera.LiveCameraContent
import com.wesee.app.ui.screens.pcd.home.camera.rememberCameraSession
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.WeSeeCard
import com.wesee.app.ui.theme.WeSeeDeep

private val HeaderShape = GenericShape { size, _ ->
    val w = size.width; val h = size.height
    moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h * 0.80f)
    cubicTo(w, h * 0.98f, w * 0.90f, h * 0.99f, w * 0.80f, h * 0.88f)
    cubicTo(w * 0.65f, h * 0.74f, w * 0.40f, h * 0.95f, w * 0.20f, h * 0.90f)
    cubicTo(w * 0.08f, h * 0.87f, 0f, h * 0.95f, 0f, h * 0.90f)
    close()
}

@Composable
private fun HomeHeader(firstName: String, systemActive: Boolean) {
    Column(
        Modifier.fillMaxWidth().clip(HeaderShape).background(Brush.verticalGradient(listOf(WeSeeDeep, WeSeeBlue)))
            .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 84.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(WeSeeCard))
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (systemActive) Color(0xFF00C853) else Color(0xFFE53935)))
            Spacer(Modifier.width(5.dp))
            WeSeeText(if (systemActive) "Sistema ativo" else "Sistema desativado", Color.White, 10, FontWeight.SemiBold)
        }
        WeSeeText("Olá $firstName.", Color.White, 38, FontWeight.SemiBold, textAlign = TextAlign.Start)
    }
}

/** Home do usuário PCD visual. [firstName] vem do usuário autenticado. */
@Composable
fun PcdHomeScreen(firstName: String, onOpenChat: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { NotesStore(context) }
    val notes = remember { mutableStateListOf(*store.load().toTypedArray()) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val battery = rememberBatteryState()
    var systemActive by remember { mutableStateOf(false) }
    val camera = rememberCameraSession()
    var cameraFullscreen by rememberSaveable { mutableStateOf(false) }

    // localização REAL do celular (Fused Location) + endereço aproximado pelo backend (geocodificação reversa)
    val userStore = remember { UserStore(context) }
    val access = rememberLocationAccess()
    val fix = rememberUserFix(access.hasPermission, intervalMs = 10_000L, minDistanceMeters = 10f).value
    val address = rememberAddress(fix?.point, userStore.token).value
    val locationInfo = LocationInfo(
        street = address.address?.label,
        accuracyMeters = fix?.accuracyMeters?.roundToInt(),
        status = when {
            !access.hasPermission -> "Toque para permitir a localização"
            fix == null && !access.gpsEnabled -> "GPS desligado. Toque para ligar"
            fix == null -> "Obtendo localização..."
            address.failed -> "Endereço indisponível"
            else -> "Buscando endereço..."
        },
    )
    val onLocationClick: (() -> Unit)? = when {
        !access.hasPermission -> access.requestPermission
        fix == null && !access.gpsEnabled -> access.openGpsSettings
        else -> null
    }

    Box(Modifier.fillMaxSize().background(WeSeeCard)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                HomeHeader(firstName, systemActive)
                Column(Modifier.overlapTop(70.dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // câmera real + IA que descreve o ambiente por voz
                    CameraArea(tiltDegrees = 0f) {
                        if (cameraFullscreen) FullscreenPlaceholder()
                        else LiveCameraContent(camera, onExpand = { cameraFullscreen = true }, onActiveChange = { systemActive = it })
                    }
                    LocationCard(locationInfo, onClick = onLocationClick)
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BatteryCard(battery, Modifier.weight(1f).fillMaxHeight())
                        NotesShortcutCard({ showEditor = true }, Modifier.weight(0.85f).fillMaxHeight())
                    }
                    NotesMural(notes)
                    Spacer(Modifier.height(4.dp))
                }
            }
            PcdBottomBar(PcdTab.Home, onHome = {}, onChat = onOpenChat)
        }
        if (cameraFullscreen) {
            FullscreenCamera(camera, onExit = { cameraFullscreen = false }, onActiveChange = { systemActive = it })
        }
        if (showEditor) {
            NoteEditorOverlay(
                onDismiss = { showEditor = false },
                onSave = { note -> notes.add(0, note); store.save(notes); showEditor = false },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable private fun HomePreview() = PcdHomeScreen("Lucas")
