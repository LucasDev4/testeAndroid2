package com.wesee.app.ui.screens.pcd.chat

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wesee.app.R
import com.wesee.app.ui.components.PcdBottomBar
import com.wesee.app.ui.components.PcdTab
import com.wesee.app.ui.components.WeSeeText
import com.wesee.app.ui.theme.IstokWeb
import com.wesee.app.ui.theme.WeSeeBlue
import com.wesee.app.ui.theme.WeSeeCard
import com.wesee.app.ui.theme.WeSeeDeep
import com.wesee.app.ui.theme.WeSeeMid
import com.wesee.app.voice.VoiceAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Estado da conversa (100% local: sem IA, sem rede). Tocar numa sugestão adiciona a pergunta e, após uma
 * pequena pausa ("digitando..."), uma resposta pré-programada escolhida por [ReplyRotator].
 */
@Stable
class NaviChatState(private val scope: CoroutineScope, private val onReply: (String) -> Unit) {
    val messages = mutableStateListOf<ChatMessage>()
    var replying by mutableStateOf(false); private set
    /** true enquanto houver conversa: o microfone some e a área central mostra as mensagens. */
    val started: Boolean get() = messages.isNotEmpty()

    private val rotator = ReplyRotator()
    private var nextId = 0L

    fun onSuggestion(suggestion: ChatSuggestion) {
        if (replying) return
        replying = true
        messages.add(ChatMessage(nextId++, suggestion.title, fromUser = true))
        scope.launch {
            delay(650)
            val reply = rotator.next(suggestion)
            replying = false
            messages.add(ChatMessage(nextId++, reply, fromUser = false))
            onReply(reply)
        }
    }

    /** "Nova conversa": limpa o histórico e volta ao estado inicial (com o microfone). */
    fun reset() {
        if (!replying) messages.clear()
    }
}

@Composable
private fun Glow() {
    Canvas(Modifier.fillMaxSize()) {
        val a = Offset(size.width, size.height * 0.03f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF5E9BD6).copy(alpha = 0.75f), Color.Transparent), a, size.width * 0.45f),
            radius = size.width * 0.45f, center = a)
        val b = Offset(0f, size.height * 0.62f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF6FA8E8).copy(alpha = 0.40f), Color.Transparent), b, size.width * 0.35f),
            radius = size.width * 0.35f, center = b)
    }
}

/** Logo (olhinhos), nome e subtítulo do Navi. */
@Composable
private fun NaviHeader() {
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp)) {
        Image(painterResource(R.drawable.wesee_eyes_chat), null, Modifier.width(100.dp))
        WeSeeText("Navi", WeSeeBlue, 48, FontWeight.Bold, textAlign = TextAlign.Start, fontFamily = IstokWeb, letterSpacing = 5.sp)
        WeSeeText("Seu parceiro pessoal", WeSeeMid, 17, textAlign = TextAlign.Start, fontFamily = IstokWeb)
    }
}

/** Estado inicial: botão circular do microfone (apenas visual por enquanto). Encolhe se a tela for baixa. */
@Composable
private fun MicArea() {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = minOf(176.dp, maxHeight - 12.dp).coerceAtLeast(96.dp)
        Box(
            Modifier.size(diameter)
                .shadow(14.dp, CircleShape, ambientColor = WeSeeBlue.copy(alpha = 0.3f), spotColor = WeSeeBlue.copy(alpha = 0.3f))
                .clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFFEAF3FD), Color(0xFF8DBFFF))))
                .semantics { contentDescription = "Microfone do Navi. Conversa por voz em breve. Use as sugestões abaixo." },
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(R.drawable.ic_mic_blue), null, Modifier.size(34.dp)) }
    }
}

// ---------------------------------------------------------------- conversa

@Composable
private fun Bubble(message: ChatMessage, maxWidth: Dp, announce: Boolean) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(240)) }
    val user = message.fromUser
    val shape = if (user) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    Column(
        Modifier.fillMaxWidth().graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 18.dp.toPx() },
        horizontalAlignment = if (user) Alignment.End else Alignment.Start,
    ) {
        if (!user) WeSeeText("Navi", WeSeeMid, 11, FontWeight.SemiBold, Modifier.padding(start = 6.dp, bottom = 2.dp), textAlign = TextAlign.Start)
        Box(
            Modifier.widthIn(max = maxWidth).clip(shape).background(if (user) WeSeeBlue else Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics {
                    contentDescription = (if (user) "Você disse: " else "Navi respondeu: ") + message.text
                    if (announce) liveRegion = LiveRegionMode.Polite
                },
        ) {
            WeSeeText(message.text, if (user) Color.White else WeSeeDeep, 14, FontWeight.Medium, textAlign = TextAlign.Start, lineHeight = 19)
        }
    }
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)).background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp).semantics { contentDescription = "Navi está escrevendo" },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) { i ->
            val a by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.size(7.dp).clip(CircleShape).background(WeSeeMid.copy(alpha = a)))
        }
    }
}

@Composable
private fun NewChatChip(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Limpar a conversa e voltar ao início", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 14.dp, vertical = 7.dp)) {
            WeSeeText("Nova conversa", WeSeeBlue, 12, FontWeight.Bold)
        }
    }
}

/** Estado 2: a área central vira uma conversa (rola sozinha até a última mensagem). */
@Composable
private fun Conversation(chat: NaviChatState) {
    val listState = rememberLazyListState()
    val count = chat.messages.size + if (chat.replying) 1 else 0
    LaunchedEffect(count) { if (count > 0) listState.animateScrollToItem(count - 1) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxBubble = maxWidth * 0.78f
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(end = 16.dp), horizontalArrangement = Arrangement.End) {
                NewChatChip(enabled = !chat.replying) { chat.reset() }
            }
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val lastId = chat.messages.lastOrNull()?.id
                items(chat.messages, key = { it.id }) { m -> Bubble(m, maxBubble, announce = !m.fromUser && m.id == lastId) }
                if (chat.replying) item(key = "typing") { TypingBubble() }
            }
        }
    }
}

// ---------------------------------------------------------------- sugestões

@Composable
private fun SuggestionCard(suggestion: ChatSuggestion, height: Dp, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(90), label = "press")
    val background by animateColorAsState(if (pressed) WeSeeMid else WeSeeBlue, tween(90), label = "pressBg")
    Box(
        modifier.height(height).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp)).background(background).alpha(if (enabled) 1f else 0.6f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Sugestão: ${suggestion.title} Toque para ver a resposta do Navi." }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        WeSeeText(suggestion.title, Color.White, suggestion.textSizeSp, FontWeight.SemiBold, lineHeight = (suggestion.textSizeSp * 1.25f).toInt())
    }
}

/** Grid assimétrico: duas linhas de três cards com larguras (e alturas por linha) diferentes. */
@Composable
private fun SuggestionsSection(disabled: Boolean, onPick: (ChatSuggestion) -> Unit) {
    val rowHeights = listOf(56.dp, 64.dp)
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 10.dp)) {
        WeSeeText("Sugestões", WeSeeBlue, 20, modifier = Modifier.fillMaxWidth(), fontFamily = IstokWeb)
        Spacer(Modifier.height(12.dp))
        NaviChatData.rows.forEachIndexed { r, row ->
            if (r > 0) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    SuggestionCard(s, rowHeights[r], enabled = !disabled, modifier = Modifier.weight(s.widthWeight)) { onPick(s) }
                }
            }
        }
    }
}

/** Campo de mensagem: mantido na composição, mas DESATIVADO (o Navi só responde pelas sugestões). */
@Composable
private fun MessageBar() {
    val pill = RoundedCornerShape(50)
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth().shadow(6.dp, pill).clip(pill)
            .background(Color.White.copy(alpha = 0.92f)).padding(start = 18.dp, end = 7.dp, top = 7.dp, bottom = 7.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Campo de mensagem desativado. Toque em uma sugestão para conversar com o Navi."
                disabled()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeSeeText("Toque em uma sugestão para conversar...", Color(0xFF5F6B78), 12, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFB7C6D8)), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_arrow_up), null, Modifier.size(20.dp))
        }
    }
}

private fun touchExplorationOn(context: Context): Boolean =
    (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true

/** Chat do Navi (PCD visual): respostas pré-programadas e locais. Estado 1 = microfone; estado 2 = conversa. */
@Composable
fun PcdChatScreen(onOpenHome: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val assistant = remember { VoiceAssistant(context) }
    DisposableEffect(Unit) { onDispose { assistant.release() } }
    // Lê a resposta em voz alta, a menos que o TalkBack já esteja lendo (evita fala duplicada)
    val chat = remember { NaviChatState(scope) { text -> if (!touchExplorationOn(context)) assistant.speak(text) } }

    Column(Modifier.fillMaxSize().background(WeSeeCard)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Glow()
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                NaviHeader()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Crossfade(targetState = chat.started, animationSpec = tween(280), label = "chat-state") { started ->
                        if (started) Conversation(chat) else MicArea()
                    }
                }
                SuggestionsSection(disabled = chat.replying) { chat.onSuggestion(it) }
            }
        }
        MessageBar()
        PcdBottomBar(PcdTab.Chat, onHome = onOpenHome, onChat = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable private fun ChatPreview() = PcdChatScreen()
