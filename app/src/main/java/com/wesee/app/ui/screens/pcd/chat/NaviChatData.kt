package com.wesee.app.ui.screens.pcd.chat

import androidx.annotation.StringRes
import com.wesee.app.R
import com.wesee.app.settings.tr
import kotlin.random.Random

/** Sugestão do chat. [widthWeight] = largura relativa do card na linha (o grid é assimétrico); [textSizeSp] = fonte proporcional ao card. */
data class ChatSuggestion(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val replyRes: List<Int>,
    val widthWeight: Float,
    val textSizeSp: Int,
)

data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean)

/**
 * TODAS as sugestões e respostas do Navi (100% local, sem IA e sem rede), como recursos de texto (strings.xml / values-en): o idioma
 * é resolvido na hora de exibir. Cada sugestão tem 5 respostas.
 * As respostas não inventam localização, clima, rotas, compromissos nem anotações: só orientam sobre os recursos do app.
 */
object NaviChatData {

    val suggestions: List<ChatSuggestion> = listOf(
        ChatSuggestion(
            id = "onde_estou", titleRes = R.string.chat_s1_title, widthWeight = 0.27f, textSizeSp = 12,
            replyRes = listOf(
                R.string.chat_s1_r1, R.string.chat_s1_r2, R.string.chat_s1_r3, R.string.chat_s1_r4, R.string.chat_s1_r5,
            ),
        ),
        ChatSuggestion(
            id = "tempo", titleRes = R.string.chat_s2_title, widthWeight = 0.34f, textSizeSp = 13,
            replyRes = listOf(
                R.string.chat_s2_r1, R.string.chat_s2_r2, R.string.chat_s2_r3, R.string.chat_s2_r4, R.string.chat_s2_r5,
            ),
        ),
        ChatSuggestion(
            id = "rota", titleRes = R.string.chat_s3_title, widthWeight = 0.39f, textSizeSp = 13,
            replyRes = listOf(
                R.string.chat_s3_r1, R.string.chat_s3_r2, R.string.chat_s3_r3, R.string.chat_s3_r4, R.string.chat_s3_r5,
            ),
        ),
        ChatSuggestion(
            id = "hoje", titleRes = R.string.chat_s4_title, widthWeight = 0.37f, textSizeSp = 13,
            replyRes = listOf(
                R.string.chat_s4_r1, R.string.chat_s4_r2, R.string.chat_s4_r3, R.string.chat_s4_r4, R.string.chat_s4_r5,
            ),
        ),
        ChatSuggestion(
            id = "anotacoes", titleRes = R.string.chat_s5_title, widthWeight = 0.32f, textSizeSp = 12,
            replyRes = listOf(
                R.string.chat_s5_r1, R.string.chat_s5_r2, R.string.chat_s5_r3, R.string.chat_s5_r4, R.string.chat_s5_r5,
            ),
        ),
        ChatSuggestion(
            id = "orientacao", titleRes = R.string.chat_s6_title, widthWeight = 0.31f, textSizeSp = 12,
            replyRes = listOf(
                R.string.chat_s6_r1, R.string.chat_s6_r2, R.string.chat_s6_r3, R.string.chat_s6_r4, R.string.chat_s6_r5,
            ),
        ),
    )

    /** Seis cards em duas linhas de três (larguras diferentes em cada linha). */
    val rows: List<List<ChatSuggestion>> = suggestions.chunked(3)
}

/**
 * Escolhe a resposta de cada sugestão de forma INDEPENDENTE: sorteia sem reposição (todas as 5 respostas saem antes de
 * qualquer repetição) e nunca repete imediatamente a última resposta daquele card, nem na virada de um ciclo para o outro.
 */
class ReplyRotator(private val random: Random = Random.Default) {
    private val bags = mutableMapOf<String, ArrayDeque<Int>>()
    private val last = mutableMapOf<String, Int>()

    fun next(suggestion: ChatSuggestion): String {
        val size = suggestion.replyRes.size
        require(size > 0) { "A sugestão '${suggestion.id}' não tem respostas" }
        val bag = bags.getOrPut(suggestion.id) { ArrayDeque() }
        if (bag.isEmpty()) {
            val fresh = (0 until size).shuffled(random).toMutableList()
            val previous = last[suggestion.id]
            if (size > 1 && fresh.first() == previous) { // evita repetir a última na virada do ciclo
                val swap = 1 + random.nextInt(size - 1)
                fresh[0] = fresh[swap].also { fresh[swap] = fresh[0] }
            }
            bag.addAll(fresh)
        }
        val index = bag.removeFirst()
        last[suggestion.id] = index
        return tr(suggestion.replyRes[index])
    }
}
