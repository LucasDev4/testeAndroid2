package com.wesee.app.ui.screens.pcd.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wesee.app.data.Note
import com.wesee.app.data.NoteColors
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.theme.HostGrotesk
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.WeSeeField
import com.wesee.app.ui.theme.WeSeeInk
import com.wesee.app.ui.theme.WeSeeInkDeep
import com.wesee.app.ui.theme.WeSeeSurface
import com.wesee.app.ui.theme.WeSeeMid
import androidx.compose.ui.res.stringResource
import com.wesee.app.R

private val HighlightStyle = SpanStyle(background = Color(0xFF004789), color = Color.White, fontWeight = FontWeight.Bold)

private fun AnnotatedString.Builder.applyHighlights(length: Int, ranges: List<Pair<Int, Int>>) {
    ranges.forEach { (s, e) ->
        val a = s.coerceIn(0, length)
        val b = e.coerceIn(a, length)
        if (b > a) addStyle(HighlightStyle, a, b)
    }
}

/** Texto da anotação com os trechos destacados. */
fun Note.toAnnotated(): AnnotatedString = buildAnnotatedString {
    append(text)
    applyHighlights(text.length, highlights)
}

private fun highlightTransformation(ranges: List<Pair<Int, Int>>) = VisualTransformation { text ->
    TransformedText(
        buildAnnotatedString { append(text.text); applyHighlights(text.length, ranges) },
        OffsetMapping.Identity,
    )
}

@Composable
private fun ActionChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.alpha(if (enabled) 1f else 0.45f).clip(shape).background(WeSeeField)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    ) { WeSeeText(label, WeSeeInk, 12, FontWeight.Bold) }
}

/** Modal centralizado (dentro do frame) para criar anotação: texto, destaque de trecho e cor. */
@Composable
fun NoteEditorOverlay(onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var highlights by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var colorIndex by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val shape = RoundedCornerShape(24.dp)
    val textStyle = TextStyle(color = WeSeeInkDeep, fontSize = 15.sp, lineHeight = 20.sp, fontFamily = HostGrotesk)
    val canSave = field.text.isNotBlank()
    val colorLabels = NoteColors.indices.map { stringResource(R.string.note_color_n, it + 1) }
    val selectedSuffix = stringResource(R.string.note_color_selected_suffix)
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier.fillMaxSize().background(Color(0x99001A33))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .statusBarsPadding().navigationBarsPadding().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth().shadow(16.dp, shape).clip(shape).background(WeSeeSurface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            WeSeeText(stringResource(R.string.note_new_title), WeSeeInk, 20, FontWeight.Bold, textAlign = TextAlign.Start)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 110.dp).clip(RoundedCornerShape(14.dp)).background(WeSeeField).padding(12.dp)) {
                BasicTextField(
                    value = field,
                    onValueChange = { if (it.text != field.text) highlights = emptyList(); field = it },
                    textStyle = textStyle, cursorBrush = SolidColor(WeSeeInk),
                    visualTransformation = highlightTransformation(highlights),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { inner ->
                        Box {
                            if (field.text.isEmpty()) BasicText(stringResource(R.string.note_placeholder), style = textStyle.copy(color = Color(0xFF9AA7B4)))
                            inner()
                        }
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            WeSeeText(stringResource(R.string.note_hint), WeSeeMid, 11, textAlign = TextAlign.Start)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip(stringResource(R.string.note_highlight), enabled = !field.selection.collapsed) {
                    highlights = highlights + (field.selection.min to field.selection.max)
                }
                if (highlights.isNotEmpty()) ActionChip(stringResource(R.string.note_clear_highlights), enabled = true) { highlights = emptyList() }
            }
            Spacer(Modifier.height(14.dp))
            WeSeeText(stringResource(R.string.note_color), WeSeeInk, 12, FontWeight.SemiBold, textAlign = TextAlign.Start)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NoteColors.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(c))
                            .then(if (i == colorIndex) Modifier.border(3.dp, WeSeeInk, CircleShape) else Modifier)
                            .clickable(role = Role.RadioButton) { colorIndex = i }
                            .semantics { contentDescription = colorLabels[i] + if (i == colorIndex) selectedSuffix else "" },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().height(46.dp).alpha(if (canSave) 1f else 0.5f).clip(RoundedCornerShape(14.dp)).background(WeSeeBlue)
                    .clickable(enabled = canSave, role = Role.Button) {
                        val text = field.text.trimEnd()
                        onSave(Note(System.currentTimeMillis(), text, NoteColors[colorIndex], highlights))
                    },
                contentAlignment = Alignment.Center,
            ) { WeSeeText(stringResource(R.string.note_add), Color.White, 15, FontWeight.Bold) }
            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onDismiss), contentAlignment = Alignment.Center) {
                WeSeeText(stringResource(R.string.action_cancel), WeSeeMid, 13, FontWeight.SemiBold)
            }
        }
    }
}
