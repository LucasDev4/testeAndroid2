package com.wesee.app.voice

import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class VoiceState { Idle, Speaking, Listening, Processing }
enum class FieldKind { Name, Email, Password, ConfirmPassword }
data class VoiceStep(val key: FieldKind, val question: String, val secret: Boolean = false)

/**
 * Conversa guiada: IA pergunta (voz) → usuário toca no microfone e responde → reconhecimento →
 * confirmação (nome/e-mail: "sim/não"; senha: não é repetida em voz alta) → próxima pergunta.
 */
class VoiceSession(
    private val assistant: VoiceAssistant,
    private val steps: List<VoiceStep>,
    private val intro: String,
    private val finishMessage: String,
    private val onFinished: (Map<FieldKind, String>) -> Unit,
) {
    var state by mutableStateOf(VoiceState.Idle)
        private set
    private var index = 0
    private var started = false
    private var done = false
    private var pending: String? = null
    private val answers = mutableMapOf<FieldKind, String>()

    fun begin() {
        if (started) return
        started = true
        say("$intro ${steps[0].question} Toque no botão grande para responder.")
    }

    fun announce(text: String) = say(text)

    fun onMicTap() {
        if (done) return
        when (state) {
            VoiceState.Speaking -> { assistant.stopSpeaking(); listen() }
            VoiceState.Idle -> listen()
            else -> Unit
        }
    }

    private fun say(text: String, then: () -> Unit = {}) {
        state = VoiceState.Speaking
        assistant.speak(text) { state = VoiceState.Idle; then() }
    }

    private fun listen() {
        state = VoiceState.Listening
        assistant.beep(Beep.Start)
        assistant.listen(processing = { state = VoiceState.Processing }, result = ::handle, failure = ::fail)
    }

    private fun handle(text: String) {
        state = VoiceState.Processing
        pending?.let { return confirm(it, text) }
        val step = steps[index]
        val value = normalize(step.key, text)
        if (value.isBlank()) return fail(SpeechRecognizer.ERROR_NO_MATCH)
        when {
            step.key == FieldKind.ConfirmPassword ->
                if (value == answers[FieldKind.Password]) {
                    answers[step.key] = value
                    assistant.beep(Beep.Success)
                    say("Senhas confirmadas.") { next() }
                } else {
                    assistant.beep(Beep.Error)
                    index = steps.indexOfFirst { it.key == FieldKind.Password }
                    say("As senhas não são iguais. ${steps[index].question}")
                }
            step.secret -> {
                answers[step.key] = value
                assistant.beep(Beep.Success)
                say("Senha recebida.") { next() }
            }
            else -> {
                pending = value
                assistant.beep(Beep.Success)
                say("Entendi: ${spoken(value)}. Está correto? Toque no botão e diga sim ou não.")
            }
        }
    }

    private fun confirm(value: String, text: String) {
        val words = text.lowercase().split(Regex("\\W+"))
        val no = words.any { it in setOf("não", "nao", "errado", "errada") }
        val yes = words.any { it in setOf("sim", "correto", "certo", "isso", "confirmo") }
        when {
            yes && !no -> {
                pending = null
                answers[steps[index].key] = value
                say("Ótimo.") { next() }
            }
            no -> { pending = null; say("Tudo bem. ${steps[index].question}") }
            else -> fail(SpeechRecognizer.ERROR_NO_MATCH, "Não entendi. Toque no botão e diga sim ou não.")
        }
    }

    private fun next() {
        index++
        if (index >= steps.size) finish() else say(steps[index].question)
    }

    private fun finish() {
        done = true
        assistant.beep(Beep.Success)
        say(finishMessage) { onFinished(answers.toMap()) }
    }

    private fun fail(code: Int, message: String? = null) {
        assistant.beep(Beep.Error)
        say(message ?: when (code) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "Não consegui ouvir. Toque no botão e responda novamente."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Sem conexão para reconhecer a voz. Verifique a internet e tente de novo."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Preciso da permissão do microfone. Ative nas configurações do aplicativo."
            -1 -> "O reconhecimento de voz não está disponível neste aparelho."
            else -> "Ocorreu um erro. Toque no botão e tente novamente."
        })
    }

    private fun normalize(key: FieldKind, text: String): String = when (key) {
        FieldKind.Name -> text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        FieldKind.Email -> text.lowercase()
            .replace(Regex("\\s*arroba\\s*"), "@").replace(Regex("\\s+ponto\\s+"), ".")
            .replace(Regex("\\s+(sublinhado|underline)\\s+"), "_").replace(Regex("\\s+traço\\s+"), "-")
            .replace(" ", "")
        FieldKind.Password, FieldKind.ConfirmPassword -> text.trim()
    }

    private fun spoken(value: String) = value.replace("@", " arroba ").replace(".", " ponto ")
}
