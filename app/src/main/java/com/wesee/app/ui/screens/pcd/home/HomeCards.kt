package com.wesee.app.ui.screens.pcd.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wesee.app.R
import com.wesee.app.data.Note
import com.wesee.app.device.BatteryState
import com.wesee.app.device.remainingLabel
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.components.softCard
import com.wesee.app.ui.theme.HostGrotesk
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.ThemedImage
import com.wesee.app.ui.theme.WeSeeMid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.wesee.app.settings.AppSettings
import com.wesee.app.ui.theme.WeSeeInk

/**
 * Dados do card de localização, todos opcionais: o que ainda não existe aparece como indisponível (nada é inventado).
 * [street] = endereço aproximado real (backend); [status] = texto exibido enquanto não há endereço (permissão, GPS, carregando).
 * Distância e passos dependem de um pedômetro que ainda não existe: ficam null e aparecem como "--".
 */
data class LocationInfo(
    val street: String?, val accuracyMeters: Int?, val distanceKm: Float? = null, val steps: Int? = null, val status: String? = null,
)

/** Texto escuro fixo das anotações: o fundo delas é pastel em qualquer tema. */
private val NoteInk = Color(0xFF002A55)

@Composable
private fun MetricBubble(text: String) {
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFF86A7C9))
            drawCircle(Color(0xFF0B3A6E), radius = size.minDimension / 2 - 1.dp.toPx(),
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))))
        }
        WeSeeText(text, Color.White, 9, FontWeight.Bold, lineHeight = 11)
    }
}

@Composable
fun LocationCard(info: LocationInfo, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(modifier.fillMaxWidth().softCard().then(clickable).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            WeSeeText(stringResource(R.string.loc_current_title), WeSeeMid, 16, FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
            Spacer(Modifier.height(8.dp))
            WeSeeText(info.street ?: info.status ?: stringResource(R.string.loc_unavailable), WeSeeInk, 21, FontWeight.Bold, Modifier.fillMaxWidth())
            if (info.accuracyMeters != null) {
                WeSeeText(stringResource(R.string.loc_accuracy, info.accuracyMeters), WeSeeMid, 10, FontWeight.SemiBold, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricBubble((if (info.distanceKm != null) "${info.distanceKm}" else "--") + "\nkm")
            MetricBubble((if (info.steps != null) "${info.steps}" else "--") + "\n" + stringResource(R.string.unit_steps))
        }
    }
}

@Composable
fun BatteryCard(battery: BatteryState, modifier: Modifier = Modifier) {
    Column(modifier.softCard().padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            WeSeeText(stringResource(R.string.battery_title), WeSeeInk, 12, FontWeight.Bold)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ThemedImage(painterResource(R.drawable.ic_battery), null, Modifier.width(26.dp))
                WeSeeText("${battery.percent}%", WeSeeInk, 9, FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        WeSeeText(battery.remainingLabel(), WeSeeMid, 16, FontWeight.SemiBold, Modifier.fillMaxWidth(), lineHeight = 21)
    }
}

/** Card "Anotações": toque para criar uma nova anotação. */
@Composable
fun NotesShortcutCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val datePattern = stringResource(R.string.date_pattern_short)
    val date = remember(datePattern) { SimpleDateFormat(datePattern, AppSettings.language.locale).format(Date()) }
    val notesDesc = stringResource(R.string.notes_shortcut_desc)
    Column(
        modifier.softCard().clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = notesDesc }.padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            WeSeeText(stringResource(R.string.notes_title), WeSeeInk, 11, FontWeight.Bold)
            WeSeeText(date, WeSeeInk, 8, FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(5) { Box(Modifier.fillMaxWidth().height(3.dp).background(WeSeeBlue)) }
        }
    }
}

@Composable
private fun StickyNote(note: Note, rotation: Float) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.width(120.dp).fillMaxHeight().rotate(rotation).shadow(4.dp, shape).clip(shape).background(Color(note.color))
            .semantics { contentDescription = note.text },
    ) {
        BasicText(
            note.toAnnotated(), Modifier.padding(10.dp),
            style = TextStyle(color = NoteInk, fontSize = 11.sp, lineHeight = 14.sp, fontFamily = HostGrotesk, fontWeight = FontWeight.Medium),
            maxLines = 8, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mural dinâmico: novas anotações aparecem aqui imediatamente. Rola na horizontal. */
@Composable
fun NotesMural(notes: List<Note>, modifier: Modifier = Modifier) {
    val tilts = listOf(-3f, 2f, -2f)
    Column(modifier.fillMaxWidth().softCard().padding(vertical = 14.dp)) {
        WeSeeText(stringResource(R.string.notes_mural_title), WeSeeInk, 14, FontWeight.SemiBold, Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(10.dp))
        if (notes.isEmpty()) {
            WeSeeText(stringResource(R.string.notes_empty), WeSeeMid, 12, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
        } else {
            LazyRow(
                Modifier.fillMaxWidth().height(150.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(notes, key = { _, n -> n.id }) { i, note -> StickyNote(note, tilts[i % tilts.size]) }
            }
        }
    }
}
