package com.wesee.app.ui.screens.pcd.maps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.wesee.app.R
import com.wesee.app.data.UserStore
import com.wesee.app.location.rememberLocationAccess
import com.wesee.app.location.rememberUserFix
import com.wesee.app.maps.MapsApi
import com.wesee.app.maps.formatDistance
import com.wesee.app.maps.formatDuration
import com.wesee.app.network.GeoPlace
import com.wesee.app.network.RouteStepInfo
import com.wesee.app.ui.components.LocalPcdNav
import com.wesee.app.ui.components.PcdBottomBar
import com.wesee.app.ui.components.PcdTab
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.components.softCard
import com.wesee.app.ui.theme.HostGrotesk
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.WeSeeCard
import com.wesee.app.ui.theme.WeSeeMid
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.wesee.app.network.localizedInstruction
import com.wesee.app.ui.theme.ThemedImage
import com.wesee.app.ui.theme.WeSeeInk
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

private val ButtonText = Color.White.copy(alpha = 0.6f)
private val RouteColor = Color(0xFF7FB4F9)
/** Rota que NÃO é de pedestre (servidor sem perfil de caminhada confirmado): cor de alerta, nunca a cor da rota a pé. */
private val NonWalkingColor = Color(0xFFE53935)

@Composable
private fun PillButton(label: String, icon: Int, iconWidth: Int, onClick: () -> Unit = {}) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(WeSeeBlue).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), null, Modifier.width(iconWidth.dp))
        Spacer(Modifier.width(8.dp))
        WeSeeText(label, ButtonText, 12, FontWeight.SemiBold)
    }
}

@Composable
private fun SearchCard(
    query: String, searching: Boolean, onQuery: (String) -> Unit, onClear: () -> Unit, onSearch: () -> Unit,
) {
    val clearDesc = stringResource(R.string.action_clear)
    val textStyle = TextStyle(color = WeSeeInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = HostGrotesk)
    Row(Modifier.fillMaxWidth().softCard(24.dp).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ThemedImage(painterResource(R.drawable.ic_pin_add), null, Modifier.height(15.dp))
            Box(Modifier.width(1.dp).height(18.dp).background(WeSeeMid))
            ThemedImage(painterResource(R.drawable.ic_pin_add), null, Modifier.height(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.CenterStart) {
                WeSeeText(stringResource(R.string.maps_your_location), WeSeeMid, 12, FontWeight.SemiBold, textAlign = TextAlign.Start)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(WeSeeMid.copy(alpha = 0.5f)))
            Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = query, onValueChange = onQuery, singleLine = true,
                    textStyle = textStyle, cursorBrush = SolidColor(WeSeeInk),
                    // a pesquisa só acontece quando o usuário CONFIRMA (tecla buscar ou botão): nunca a cada tecla
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) BasicText(stringResource(R.string.maps_destination_hint), style = textStyle.copy(color = WeSeeInk.copy(alpha = 0.7f)))
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    Box(Modifier.size(34.dp).clickable(role = Role.Button, onClick = onClear).semantics { contentDescription = clearDesc }, contentAlignment = Alignment.Center) {
                        WeSeeText("✕", WeSeeMid, 14, FontWeight.Bold)
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(WeSeeBlue)
                        .clickable(enabled = !searching, role = Role.Button, onClick = onSearch)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) { WeSeeText(stringResource(if (searching) R.string.maps_searching else R.string.maps_search), ButtonText, 11, FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun PlaceList(items: List<GeoPlace>, onPick: (GeoPlace) -> Unit) {
    Column(Modifier.fillMaxWidth().softCard(18.dp)) {
        items.take(8).forEach { p ->
            Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(p) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                WeSeeText(p.title, WeSeeInk, 13, FontWeight.Bold, textAlign = TextAlign.Start)
                if (p.subtitle.isNotBlank()) WeSeeText(p.subtitle, WeSeeMid, 10, textAlign = TextAlign.Start)
            }
        }
    }
}

/** Aviso em card (permissão, GPS, "nenhum resultado"...), com ação opcional. */
@Composable
private fun NoticeCard(text: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().softCard(18.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        WeSeeText(text, WeSeeInk, 11, FontWeight.SemiBold, Modifier.weight(1f), textAlign = TextAlign.Start)
        if (actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(50)).background(WeSeeBlue).clickable(role = Role.Button, onClick = onAction)
                .padding(horizontal = 14.dp, vertical = 8.dp)) { WeSeeText(actionLabel, ButtonText, 11, FontWeight.SemiBold) }
        }
    }
}

/** Etapas da rota. Só entram trechos com instrução OU nome de via: nada é inventado quando o OSRM não deu informação. */
@Composable
private fun StepsPanel(steps: List<RouteStepInfo>) {
    val shown = steps.filter { localizedInstruction(it) != null || !it.streetName.isNullOrBlank() }
    if (shown.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().heightIn(max = 170.dp).softCard(18.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        shown.forEach { s ->
            val text = (localizedInstruction(s) ?: s.streetName.orEmpty()) + if (s.distanceMeters > 0) " · ${formatDistance(s.distanceMeters)}" else ""
            WeSeeText(text, WeSeeInk, 11, FontWeight.SemiBold, Modifier.fillMaxWidth().padding(vertical = 4.dp), textAlign = TextAlign.Start)
        }
    }
}

/**
 * Mapas do PCD: Google Maps SDK só desenha (mapa, marcador do destino, linha da rota). A pesquisa de destino
 * (Nominatim) e a rota (OSRM) vêm do backend; a posição vem do Fused Location Provider.
 */
@Composable
fun PcdMapsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val nav = LocalPcdNav.current
    val userStore = remember { UserStore(context) }
    val state = remember { MapsState { userStore.token } }

    val access = rememberLocationAccess()
    val user = rememberUserFix(access.hasPermission).value?.point

    var showSteps by rememberSaveable { mutableStateOf(false) }
    val camera = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    var centered by remember { mutableStateOf(false) }
    val padPx = with(LocalDensity.current) { 90.dp.roundToPx() }
    val mapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) }

    val doSearch: () -> Unit = {
        focus.clearFocus()
        scope.launch { state.search(user) }
    }

    // rota: origem = posição atual, destino = lugar escolhido (recalcula só quando o destino muda ou a 1ª posição chega)
    LaunchedEffect(state.destination, user != null) {
        if (state.destination != null && user != null) state.calculateRoute(user)
    }
    // centraliza na posição atual (uma vez)
    LaunchedEffect(user, mapLoaded) {
        if (user != null && mapLoaded && !centered && state.destination == null) {
            camera.animate(CameraUpdateFactory.newLatLngZoom(user, 16f)); centered = true
        }
    }
    // ajusta o zoom para mostrar a rota inteira
    LaunchedEffect(state.route, mapLoaded) {
        val r = state.route
        if (r != null && mapLoaded && r.points.size > 1) {
            val b = LatLngBounds.builder()
            r.points.forEach { b.include(it) }
            user?.let { b.include(it) }
            try { camera.animate(CameraUpdateFactory.newLatLngBounds(b.build(), padPx)) } catch (e: IllegalStateException) { }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B1622))) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
                properties = MapProperties(isMyLocationEnabled = access.hasPermission, mapStyleOptions = mapStyle),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, compassEnabled = false, mapToolbarEnabled = false),
                contentPadding = PaddingValues(bottom = 230.dp),
                onMapLoaded = { mapLoaded = true },
            ) {
                state.destination?.let { d -> Marker(state = remember(d) { MarkerState(d.latLng) }, title = d.title) }
                state.route?.let { Polyline(points = it.points, color = if (it.walking) RouteColor else NonWalkingColor, width = 14f) }
            }

            Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchCard(
                    query = state.query, searching = state.searching,
                    onQuery = { state.query = it }, onClear = { state.clear() }, onSearch = doSearch,
                )
                if (state.results.isNotEmpty()) {
                    PlaceList(state.results) { p -> state.select(p); focus.clearFocus() }
                }
                state.searchMessage?.let { NoticeCard(it) }
                when {
                    !access.hasPermission -> NoticeCard(stringResource(R.string.maps_enable_location), stringResource(R.string.maps_allow), access.requestPermission)
                    !access.gpsEnabled -> NoticeCard(stringResource(R.string.maps_gps_off), stringResource(R.string.maps_settings), access.openGpsSettings)
                    user == null -> NoticeCard(stringResource(R.string.maps_getting_location))
                }
                if (!MapsApi.isConfigured) {
                    NoticeCard(stringResource(R.string.maps_no_key))
                }
            }

            // card inferior: destino + distância/tempo + avisos
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to WeSeeCard, 1f to WeSeeCard))
                    .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 14.dp),
            ) {
                val r = state.route
                if (showSteps && r != null) {
                    StepsPanel(r.steps)
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.Top) {
                    WeSeeText(state.destination?.title ?: stringResource(R.string.maps_select_destination), WeSeeInk, 24, FontWeight.Bold, Modifier.weight(1f),
                        lineHeight = 28, textAlign = TextAlign.Start)
                    if (r != null) {
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            WeSeeText(formatDistance(r.distanceMeters), WeSeeInk, 22, FontWeight.Bold)
                            WeSeeText(formatDuration(r.durationSeconds), WeSeeMid, 12, FontWeight.SemiBold)
                        }
                    } else if (state.routing) {
                        WeSeeText(stringResource(R.string.maps_calculating), WeSeeMid, 12, FontWeight.SemiBold)
                    }
                }
                if (r?.warning != null) {
                    WeSeeText(stringResource(R.string.route_non_walking_warning), NonWalkingColor, 10, FontWeight.Bold, Modifier.padding(top = 4.dp), textAlign = TextAlign.Start)
                }
                state.routeError?.let { err ->
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        WeSeeText(err, WeSeeMid, 10, FontWeight.SemiBold, Modifier.weight(1f), textAlign = TextAlign.Start)
                        if (user != null) {
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(WeSeeBlue)
                                .clickable(role = Role.Button) { scope.launch { state.calculateRoute(user) } }
                                .padding(horizontal = 12.dp, vertical = 6.dp)) { WeSeeText(stringResource(R.string.maps_try_again), ButtonText, 10, FontWeight.SemiBold) }
                        }
                    }
                }
                if (state.destination != null && user == null && r == null && state.routeError == null) {
                    WeSeeText(stringResource(R.string.maps_waiting_location), WeSeeMid, 10, textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 4.dp))
                }
                if (r?.safetyNotice != null) {
                    WeSeeText(stringResource(R.string.route_safety_notice), WeSeeMid, 9, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp))
                }
                WeSeeText(stringResource(R.string.maps_attribution), WeSeeMid, 8, textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillButton(stringResource(R.string.maps_directions), R.drawable.ic_direction, 15) { if (r != null && r.steps.isNotEmpty()) showSteps = !showSteps }
                    Spacer(Modifier.width(10.dp))
                    PillButton(stringResource(R.string.maps_routes), R.drawable.ic_arrow_left, 16)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(78.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(WeSeeBlue)
                            .clickable(role = Role.Button) { /* FUTURO: iniciar navegação */ },
                        contentAlignment = Alignment.Center,
                    ) { Image(painterResource(R.drawable.ic_turn_right), stringResource(R.string.maps_start_navigation), Modifier.size(40.dp)) }
                }
            }
        }
        PcdBottomBar(PcdTab.Maps, onHome = nav.onHome, onChat = nav.onChat)
    }
}
