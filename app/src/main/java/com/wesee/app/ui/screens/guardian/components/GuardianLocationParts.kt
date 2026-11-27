package com.wesee.app.ui.screens.guardian.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wesee.app.network.VinculoItem
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.WeSeeSurface
import com.wesee.app.ui.theme.WeSeeSurfaceAlt
import androidx.compose.ui.res.stringResource
import com.wesee.app.R
import com.wesee.app.ui.theme.WeSeeInk

/** Escolha da pessoa acompanhada (aparece quando há mais de uma vinculada). */
@Composable
fun PessoaSelector(pessoas: List<VinculoItem>, selectedId: Long, onSelect: (VinculoItem) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 18.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pessoas.forEach { p ->
            val isSelected = p.relacionadoId == selectedId
            val followDesc = stringResource(R.string.guardian_follow, p.nome)
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (isSelected) WeSeeBlue else WeSeeSurfaceAlt)
                    .clickable(role = Role.Button) { onSelect(p) }
                    .semantics { selected = isSelected; contentDescription = followDesc }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { WeSeeText(p.nome.ifBlank { stringResource(R.string.guardian_person_default) }, if (isSelected) Color.White else WeSeeInk, 11, FontWeight.SemiBold) }
        }
    }
}

/** Aviso curto sob o card de localização (ex.: "O aparelho ainda não enviou a localização."). */
@Composable
fun GuardianNotice(text: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WeSeeSurfaceAlt).padding(12.dp)) {
        WeSeeText(text, WeSeeInk, 10, FontWeight.SemiBold, textAlign = TextAlign.Start)
    }
}

@Composable
fun RefreshPill(refreshing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(WeSeeBlue)
            .clickable(enabled = !refreshing, role = Role.Button, onClickLabel = stringResource(R.string.guardian_refresh), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { WeSeeText(stringResource(if (refreshing) R.string.guardian_refreshing else R.string.guardian_refresh), Color.White, 11, FontWeight.SemiBold) }
}

/** Ocupa o lugar do mapa quando ainda não há localização para mostrar (não desenha posição inventada). */
@Composable
fun GuardianMapPlaceholder(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier.shadow(10.dp, shape).clip(shape).background(WeSeeSurface), contentAlignment = Alignment.Center) {
        WeSeeText(text, WeSeeInk, 11, FontWeight.SemiBold, Modifier.padding(16.dp))
    }
}
