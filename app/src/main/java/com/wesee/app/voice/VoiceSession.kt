package com.wesee.app.voice

import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wesee.app.R
import com.wesee.app.settings.tr
import com.wesee.app.settings.AppSettings
import com.wesee.app.settings.AppLanguage

enum class VoiceState { Idle, Speaking, Listening, Processing }
enum class FieldKind { Name, Email, Password, ConfirmPassword }
data class VoiceStep(val key: FieldKind, val question: String, val secret: Boolean = false)

private val digitWords = mapOf(
    "zero" to '0', "um" to '1', "uma" to '1', "dois" to '2', "duas" to '2', "três" to '3', "tres" to '3',
    "quatro" to '4', "cinco" to '5', "seis" to '6', "meia" to '6', "sete" to '7', "oito" to '8', "nove" to '9',
    // inglês (aceito em qualquer idioma: só vale quando a pessoa realmente diz esses números)
    "oh" to '0', "one" to '1', "two" to '2', "to" to '2', "too" to '2', "three" to '3', "four" to '4', "for" to '4',
    "five" to '5', "six" to '6', "seven" to '7', "eight" to '8', "nine" to '9',
)

/** "um dois três 4 5 seis" → "123456" (o backend exige senha com exatamente 6 números). */
fun spokenToDigits(text: String): String = buildString {
    text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).forEach { tok ->
        if (tok.isNotEmpty() && tok.all { it.isDigit() }) append(tok) else digitWords[tok]?.let { append(it) }
    }
}

/**
 * Conversa guiada: IA pergunta (voz) → usuário toca no microfone e responde → reconhecimento →
 * confirmação (nome/e-mail: "sim/não"; senha: não é repetida em voz alta) → próxima pergunta.
 * Ao responder tudo, [onFinished] é chamado (envie ao backend) e a tela deve terminar com
 * [complete], [retryFrom] ou [failSubmit].
 */
class VoiceSession(
    private val assistant: VoiceAssistant,
    private val steps: List<VoiceStep>,
    private val intro: String,
    private val onFinished: (VoiceSession, Map<FieldKind, String>) -> Unit,
) {
    var state by mutableStateOf(VoiceState.Idle)
        private set
    private var index = 0
    private var started = false
    private var done = false
    private var awaitingRetry = false
    private var pending: String? = null
    private val answers = mutableMapOf<FieldKind, String>()

    fun begin() {
        if (started) return
        started = true
        say("$intro ${steps[0].question} " + tr(R.string.voice_tap_to_answer))
    }

    fun announce(text: String) = say(text)

    fun onMicTap() {
        if (done) return
        if (awaitingRetry) { // erro de rede: reenviar as mesmas respostas
            assistant.stopSpeaking(); awaitingRetry = false; state = VoiceState.Processing
            onFinished(this, answers.toMap()); return
        }
        when (state) {
            VoiceState.Speaking -> { assistant.stopSpeaking(); listen() }
            VoiceState.Idle -> listen()
            else -> Unit
        }
    }

    /** Resposta final com sucesso (ex.: "Cadastro concluído"). */
    fun complete(message: String, then: () -> Unit = {}) {
        done = true
        assistant.beep(Beep.Success)
        say(message, then)
    }

    /** O backend recusou algo: explica e volta a perguntar a partir do campo [key]. */
    fun retryFrom(key: FieldKind, message: String) {
        done = false; awaitingRetry = false; pending = null
        index = steps.indexOfFirst { it.key == key }.coerceAtLeast(0)
        assistant.beep(Beep.Error)
        say("$message ${steps[index].question}")
    }

    /** Falha de rede/servidor: explica; o próximo toque no microfone reenvia as respostas. */
    fun failSubmit(message: String) {
        awaitingRetry = true
        assistant.beep(Beep.Error)
        say(message)
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
                    say(tr(R.string.voice_passwords_confirmed)) { next() }
                } else {
                    assistant.beep(Beep.Error)
                    index = steps.indexOfFirst { it.key == FieldKind.Password }
                    say(tr(R.string.voice_passwords_mismatch) + " " + steps[index].question)
                }
            step.key == FieldKind.Password && value.length != 6 -> {
                assistant.beep(Beep.Error)
                say(tr(R.string.voice_password_len) + " " + step.question)
            }
            step.secret -> {
                answers[step.key] = value
                assistant.beep(Beep.Success)
                say(tr(R.string.voice_password_received)) { next() }
            }
            else -> {
                pending = value
                assistant.beep(Beep.Success)
                say(tr(R.string.voice_understood_confirm, spoken(value)))
            }
        }
    }

    private fun confirm(value: String, text: String) {
        val words = text.lowercase().split(Regex("\\W+"))
        val no = words.any { it in setOf("não", "nao", "errado", "errada", "no", "nope", "wrong", "incorrect") }
        val yes = words.any { it in setOf("sim", "correto", "certo", "isso", "confirmo", "yes", "yeah", "yep", "correct", "right", "confirm") }
        when {
            yes && !no -> {
                pending = null
                answers[steps[index].key] = value
                say(tr(R.string.voice_great)) { next() }
            }
            no -> { pending = null; say(tr(R.string.voice_alright) + " " + steps[index].question) }
            else -> fail(SpeechRecognizer.ERROR_NO_MATCH, tr(R.string.voice_not_understood_yesno))
        }
    }

    private fun next() {
        index++
        if (index >= steps.size) finish() else say(steps[index].question)
    }

    private fun finish() {
        state = VoiceState.Processing
        assistant.beep(Beep.Success)
        onFinished(this, answers.toMap())
    }

    private fun fail(code: Int, message: String? = null) {
        assistant.beep(Beep.Error)
        say(message ?: when (code) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                tr(R.string.voice_err_no_match)
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                tr(R.string.voice_err_network)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                tr(R.string.voice_err_permission)
            -1 -> tr(R.string.voice_err_unavailable)
            else -> tr(R.string.voice_err_generic)
        })
    }

    private fun normalize(key: FieldKind, text: String): String = when (key) {
        FieldKind.Name -> text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        FieldKind.Email -> text.lowercase()
            .replace(Regex("\\s*arroba\\s*"), "@").replace(Regex("\\s+ponto\\s+"), ".")
            .replace(Regex("\\s+(sublinhado|underline)\\s+"), "_").replace(Regex("\\s+traço\\s+"), "-")
            .let { if (AppSettings.language == AppLanguage.EN) englishSpokenEmail(it) else it }
            .replace(" ", "")
        FieldKind.Password, FieldKind.ConfirmPassword -> spokenToDigits(text)
    }

    /** "john at gmail dot com" → "john@gmail.com" (só no modo inglês). */
    private fun englishSpokenEmail(t: String) = t.replace(Regex("\\s+at\\s+"), "@").replace(Regex("\\s+dot\\s+"), ".")
        .replace(Regex("\\s+(dash|hyphen)\\s+"), "-")

    private fun spoken(value: String): String =
        if (AppSettings.language == AppLanguage.EN) value.replace("@", " at ").replace(".", " dot ")
        else value.replace("@", " arroba ").replace(".", " ponto ")
}
