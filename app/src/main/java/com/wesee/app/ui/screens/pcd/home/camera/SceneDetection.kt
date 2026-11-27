package com.wesee.app.ui.screens.pcd.home.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wesee.app.data.UserStore
import com.wesee.app.network.ApiResult
import com.wesee.app.network.AtlasApi
import com.wesee.app.network.DetectionResult
import com.wesee.app.voice.VoiceAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import com.wesee.app.R
import com.wesee.app.settings.tr

/** Estado da câmera/IA exibido sobre a prévia. */
enum class CameraStatus { Starting, Active, Error, SessionExpired, CameraFailed }

/**
 * Decide O QUE e QUANDO falar, para o usuário ouvir tudo que a câmera vê sem ficar repetindo a mesma frase:
 *  - perigo (prioridade ALTA e muito próximo): fala já, interrompendo, e repete a cada 5 s enquanto durar;
 *  - cena mudou: descreve tudo (no máx. a cada 3 s e sem cortar uma fala em andamento);
 *  - cena igual: lembra a descrição só a cada 20 s;
 *  - cena ficou vazia: diz "Caminho livre" uma vez.
 */
class SceneNarrator(private val assistant: VoiceAssistant) {
    private var lastSignature = ""
    private var lastSpokenAt = 0L
    @Volatile private var speaking = false
    private var announcedClear = true

    /** Devolve o texto falado (ou a descrição atual, mesmo sem falar) para mostrar na legenda. */
    fun onResult(r: DetectionResult, now: Long = SystemClock.elapsedRealtime()): String {
        val scene = SceneDescriber.scene(r)
        val alert = SceneDescriber.alert(r)
        if (r.objects.isEmpty()) {
            if (!announcedClear && lastSignature.isNotEmpty() && now - lastSpokenAt >= 2500 && !speaking) {
                say(tr(R.string.scene_clear_short), now)
                announcedClear = true
                lastSignature = ""
            }
            return scene.ifBlank { tr(R.string.scene_clear_short) }
        }
        announcedClear = false
        val signature = r.objects.map { "${it.label}|${it.position}|${it.proximity == "NEAR"}" }.sorted().joinToString(";")
        val urgent = r.objects.any { it.priority == "HIGH" && it.proximity == "NEAR" }
        val since = now - lastSpokenAt
        when {
            urgent && alert.isNotBlank() && (signature != lastSignature || since >= 5000) -> {
                say(tr(R.string.scene_alert_prefix, alert), now); lastSignature = signature
            }
            signature != lastSignature && since >= 3000 && !speaking && scene.isNotBlank() -> {
                say(scene, now); lastSignature = signature
            }
            signature == lastSignature && since >= 20000 && !speaking && scene.isNotBlank() -> say(scene, now)
        }
        return scene
    }

    fun say(text: String, now: Long = SystemClock.elapsedRealtime()) {
        speaking = true
        lastSpokenAt = now
        assistant.speak(text) { speaking = false }
    }

    val isSpeaking: Boolean get() = speaking
}

/**
 * Liga a câmera ao backend: recebe quadros do CameraX, manda 1 por vez (a cada ~1,5 s) para POST /detect
 * e entrega o resultado ao [SceneNarrator]. Se o servidor falhar, espera cada vez mais antes de tentar de novo.
 */
class SceneDetectionController(
    private val scope: CoroutineScope,
    private val userStore: UserStore,
    private val narrator: SceneNarrator,
) {
    var status by mutableStateOf(CameraStatus.Starting); private set
    var caption by mutableStateOf<String?>(null); private set
    var statusMessage by mutableStateOf<String?>(null); private set

    @Volatile private var inFlight = false
    @Volatile private var lastSentAt = 0L
    @Volatile private var failures = 0
    private var lastErrorSpokenAt = 0L
    private var greeted = false

    fun cameraReady() { if (status == CameraStatus.Starting) statusMessage = tr(R.string.ai_starting) }

    fun cameraFailed() {
        status = CameraStatus.CameraFailed
        statusMessage = tr(R.string.camera_open_failed)
        narrator.say(tr(R.string.camera_open_failed))
    }

    private fun intervalMs(): Long = min(10_000L, BASE_INTERVAL_MS shl min(failures, 3))

    /** Chamado pelo ImageAnalysis (thread de fundo) a cada quadro. Sempre fecha o quadro. */
    fun onFrame(image: ImageProxy) {
        try {
            if (status == CameraStatus.SessionExpired || inFlight) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastSentAt < intervalMs()) return
            val token = userStore.token
            if (token == null) { scope.launch { onError(401, tr(R.string.session_expired_short)) }; return }
            val jpeg = frameToJpeg(image) ?: return
            inFlight = true
            lastSentAt = now
            scope.launch {
                try {
                    handle(AtlasApi.detect(token, jpeg))
                } finally {
                    inFlight = false
                }
            }
        } finally {
            image.close()
        }
    }

    private fun handle(r: ApiResult<DetectionResult>) {
        when (r) {
            is ApiResult.Success -> {
                failures = 0
                if (status != CameraStatus.Active) {
                    status = CameraStatus.Active; statusMessage = null
                    if (!greeted) { greeted = true; narrator.say(tr(R.string.camera_on_greeting)) }
                }
                caption = narrator.onResult(r.value)
            }
            is ApiResult.Error -> onError(r.code, r.message)
        }
    }

    private fun onError(code: Int, message: String) {
        failures++
        if (code == 401 || code == 403) {
            status = CameraStatus.SessionExpired
            statusMessage = tr(R.string.session_expired_login)
            narrator.say(tr(R.string.session_expired_spoken))
            return
        }
        status = CameraStatus.Error
        statusMessage = when (code) {
            -1 -> tr(R.string.err_no_connection_short)
            502, 503, 504 -> tr(R.string.ai_unavailable)
            else -> tr(R.string.ai_analyze_failed)
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastErrorSpokenAt >= 30_000 && !narrator.isSpeaking) {
            lastErrorSpokenAt = now
            narrator.say(statusMessage.orEmpty() + " " + tr(R.string.will_retry))
        }
    }

    private companion object { const val BASE_INTERVAL_MS = 1500L }
}

/** Quadro da câmera → JPEG em pé (rotação aplicada), com no máximo 640 px no lado maior. */
internal fun frameToJpeg(image: ImageProxy, maxSide: Int = 640, quality: Int = 70): ByteArray? = try {
    val source = image.toBitmap()
    val longest = max(source.width, source.height)
    val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
    val matrix = Matrix().apply {
        postRotate(image.imageInfo.rotationDegrees.toFloat())
        postScale(scale, scale)
    }
    val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }
} catch (e: Exception) {
    null
}
