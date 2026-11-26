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

/** Escolha da pessoa acompanhada (aparece quando há mais de uma vinculada). */
@Composable
fun PessoaSelector(pessoas: List<VinculoItem>, selectedId: Long, onSelect: (VinculoItem) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 18.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pessoas.forEach { p ->
            val isSelected = p.relacionadoId == selectedId
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (isSelected) WeSeeBlue else Color(0xFFEFF5FB))
                    .clickable(role = Role.Button) { onSelect(p) }
                    .semantics { selected = isSelected; contentDescription = "Acompanhar ${p.nome}" }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { WeSeeText(p.nome.ifBlank { "Pessoa" }, if (isSelected) Color.White else WeSeeBlue, 11, FontWeight.SemiBold) }
        }
    }
}

/** Aviso curto sob o card de localização (ex.: "O aparelho ainda não enviou a localização."). */
@Composable
fun GuardianNotice(text: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFEFF5FB)).padding(12.dp)) {
        WeSeeText(text, WeSeeBlue, 10, FontWeight.SemiBold, textAlign = TextAlign.Start)
    }
}

@Composable
fun RefreshPill(refreshing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(WeSeeBlue)
            .clickable(enabled = !refreshing, role = Role.Button, onClickLabel = "Atualizar localização", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { WeSeeText(if (refreshing) "Atualizando..." else "Atualizar localização", Color.White, 11, FontWeight.SemiBold) }
}

/** Ocupa o lugar do mapa quando ainda não há localização para mostrar (não desenha posição inventada). */
@Composable
fun GuardianMapPlaceholder(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier.shadow(10.dp, shape).clip(shape).background(Color(0xFFE9EAEC)), contentAlignment = Alignment.Center) {
        WeSeeText(text, WeSeeBlue, 11, FontWeight.SemiBold, Modifier.padding(16.dp))
    }
}
