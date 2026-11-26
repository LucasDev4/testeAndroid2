package com.wesee.app.network

import com.google.android.gms.maps.model.LatLng
import com.wesee.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object UserType {
    const val PCD_VISUAL = "PCD_VISUAL"
    const val RESPONSAVEL = "RESPONSAVEL"
}

data class AuthSession(val token: String, val nome: String, val email: String, val tipo: String)
data class RegisterBody(
    val nome: String, val email: String, val senha: String,
    val cpf: String? = null, val telefone: String? = null, val tipo: String = UserType.PCD_VISUAL,
)
/** Vínculo (GET /api/vinculos): "o outro lado" da relação. status: ATIVO | PENDENTE (reservado) | REVOGADO. */
data class VinculoItem(val id: Long, val relacionadoId: Long, val nome: String, val tipo: String, val status: String)
/** Código temporário gerado pelo backend (POST /api/vinculos/codigos). */
data class CodigoGerado(val codigo: String, val validadeSegundos: Long)
/** Objeto detectado pela IA (já classificado pelo backend). position: LEFT|CENTER|RIGHT, proximity: FAR|MEDIUM|NEAR, priority: LOW|MEDIUM|HIGH. */
data class DetectedItem(
    val className: String, val label: String, val confidence: Double,
    val position: String, val proximity: String, val priority: String,
)
/** Resposta de POST /detect: [scene] = tudo que a câmera vê (pt-BR); [alert] = alerta do objeto mais urgente. */
data class DetectionResult(
    val objects: List<DetectedItem>, val alert: String, val alertPriority: String, val scene: String,
)
data class Profile(val id: Long, val nome: String, val cpf: String?, val email: String, val telefone: String?, val tipo: String)

/** code = -1 → sem conexão com o servidor. [fields] vem do erro 400 de validação do backend. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Error(val code: Int, val message: String, val fields: Map<String, String> = emptyMap()) : ApiResult<Nothing>
}

/** Cliente do backend Backend-atlas (Spring Boot + JWT). Endpoints: /auth/register, /auth/login, /user/me. */
object AtlasApi {
    private val client = OkHttpClient.Builder()
        // o Render gratuito pode levar ~1 min para "acordar" na primeira chamada: timeouts generosos
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS).build()
    private val json = "application/json; charset=utf-8".toMediaType()

    private suspend fun call(
        path: String, body: JSONObject?, token: String?, post: Boolean = body != null, multipart: RequestBody? = null,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder().url(BuildConfig.API_BASE_URL.trimEnd('/') + path).header("Accept", "application/json")
            token?.let { builder.header("Authorization", "Bearer $it") }
            if (multipart != null) builder.post(multipart)
            else if (post) builder.post((body?.toString() ?: "").toRequestBody(json)) else builder.get()
            client.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val obj = try { JSONObject(text) } catch (e: JSONException) { JSONObject() }
                if (resp.isSuccessful) ApiResult.Success(obj) else {
                    val f = obj.optJSONObject("fields")
                    val fields = f?.let { o -> o.keys().asSequence().associateWith { k -> o.optString(k) } } ?: emptyMap()
                    ApiResult.Error(resp.code, obj.optString("message").ifBlank { "Erro ${resp.code}" }, fields)
                }
            }
        } catch (e: IOException) {
            ApiResult.Error(-1, "Sem conexão com o servidor (${BuildConfig.API_BASE_URL}).")
        } catch (e: IllegalArgumentException) {
            ApiResult.Error(-1, "Endereço do servidor inválido (API_BASE_URL).")
        }
    }

    private fun JSONObject.str(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)

    private fun parseProfile(o: JSONObject) = Profile(
        o.optLong("id"), o.optString("nome"), o.str("cpf"), o.optString("email"), o.str("telefone"),
        o.str("tipo") ?: UserType.PCD_VISUAL,
    )

    suspend fun register(b: RegisterBody): ApiResult<Profile> {
        val body = JSONObject().put("nome", b.nome).put("email", b.email).put("senha", b.senha).put("tipo", b.tipo)
        b.cpf?.let { body.put("cpf", it) }
        b.telefone?.let { body.put("telefone", it) }
        return when (val r = call("/auth/register", body, null)) {
            is ApiResult.Success -> ApiResult.Success(parseProfile(r.value))
            is ApiResult.Error -> r
        }
    }

    suspend fun login(email: String, senha: String): ApiResult<AuthSession> {
        val body = JSONObject().put("email", email).put("senha", senha)
        return when (val r = call("/auth/login", body, null)) {
            is ApiResult.Success -> ApiResult.Success(
                AuthSession(r.value.optString("token"), r.value.optString("nome"), r.value.optString("email"),
                    r.value.str("tipo") ?: UserType.PCD_VISUAL))
            is ApiResult.Error -> r
        }
    }

    suspend fun me(token: String): ApiResult<Profile> = when (val r = call("/user/me", null, token)) {
        is ApiResult.Success -> ApiResult.Success(parseProfile(r.value))
        is ApiResult.Error -> r
    }

    /** Chamada leve só para acordar o servidor (resultado ignorado). */
    suspend fun wakeUp() { call("/v3/api-docs", null, null) }

    // ---- Vínculos PCD <-> Responsável (identidade sempre pelo token) ----

    private fun parseVinculo(o: JSONObject) = VinculoItem(
        o.optLong("id"), o.optLong("relacionadoId"), o.optString("nomeRelacionado"),
        o.optString("tipoRelacionado"), o.optString("status"),
    )

    /** PCD: seus responsáveis. Responsável: seus PCDs. (backend: apenas vínculos ativos) */
    suspend fun listarVinculos(token: String): ApiResult<List<VinculoItem>> = when (val r = call("/api/vinculos", null, token)) {
        is ApiResult.Success -> {
            val arr = r.value.optJSONArray("vinculos")
            ApiResult.Success(if (arr == null) emptyList() else (0 until arr.length()).map { parseVinculo(arr.getJSONObject(it)) })
        }
        is ApiResult.Error -> r
    }

    /** PCD: pede um código temporário. O código só é devolvido aqui, uma vez. */
    suspend fun gerarCodigo(token: String): ApiResult<CodigoGerado> = when (val r = call("/api/vinculos/codigos", null, token, post = true)) {
        is ApiResult.Success -> ApiResult.Success(CodigoGerado(r.value.optString("codigo"), r.value.optLong("validadeSegundos")))
        is ApiResult.Error -> r
    }

    /** Responsável: usa o código recebido. O servidor decide se o vínculo fica ATIVO (ou PENDENTE). */
    suspend fun solicitarVinculo(token: String, codigo: String): ApiResult<VinculoItem> =
        when (val r = call("/api/vinculos/solicitar", JSONObject().put("codigo", codigo), token)) {
            is ApiResult.Success -> ApiResult.Success(parseVinculo(r.value))
            is ApiResult.Error -> r
        }

    // ---- Endereços, rotas e localização: TUDO pelo backend (o app não fala com Nominatim/OSRM) ----

    /** Resposta cujo JSON veio fora do contrato (campo ausente...) vira erro tratável em vez de travar o app. */
    private inline fun <T> parsing(block: () -> T): ApiResult<T> =
        try { ApiResult.Success(block()) } catch (e: JSONException) { ApiResult.Error(-2, "O servidor enviou uma resposta inesperada.") }

    private fun coord(v: Double): String = String.format(Locale.US, "%.6f", v)

    private fun parsePoint(o: JSONObject) = LatLng(o.getDouble("latitude"), o.getDouble("longitude"))

    /** Pesquisa de destino (Nominatim). Chame só quando o usuário CONFIRMAR a busca, nunca a cada tecla. */
    suspend fun searchPlaces(token: String, query: String, near: LatLng?): ApiResult<PlaceSearch> {
        val nearParams = if (near == null) "" else "&lat=${coord(near.latitude)}&lon=${coord(near.longitude)}"
        val path = "/api/geocoding/search?q=" + URLEncoder.encode(query, "UTF-8") + nearParams
        return when (val r = call(path, null, token)) {
            is ApiResult.Success -> parsing {
                val arr = r.value.optJSONArray("results")
                val places = if (arr == null) emptyList() else (0 until arr.length()).map { i ->
                    val x = arr.getJSONObject(i)
                    GeoPlace(
                        x.str("name").orEmpty(), x.str("displayName").orEmpty(), parsePoint(x),
                        x.str("suburb"), x.str("city"), x.str("state"),
                    )
                }
                PlaceSearch(places, r.value.str("attribution").orEmpty())
            }
            is ApiResult.Error -> r
        }
    }

    /** Endereço aproximado de uma coordenada (geocodificação reversa). */
    suspend fun reverseGeocode(token: String, point: LatLng): ApiResult<AddressInfo> {
        val path = "/api/geocoding/reverse?lat=${coord(point.latitude)}&lon=${coord(point.longitude)}"
        return when (val r = call(path, null, token)) {
            is ApiResult.Success -> ApiResult.Success(
                AddressInfo(r.value.str("shortAddress"), r.value.str("displayName"), r.value.str("city"), r.value.str("state")))
            is ApiResult.Error -> r
        }
    }

    /** Rota (OSRM) entre origem e destino. A geometria já vem como latitude/longitude. */
    suspend fun route(token: String, origin: LatLng, destination: LatLng): ApiResult<RouteData> {
        val path = "/api/routes?originLat=${coord(origin.latitude)}&originLon=${coord(origin.longitude)}" +
            "&destinationLat=${coord(destination.latitude)}&destinationLon=${coord(destination.longitude)}"
        return when (val r = call(path, null, token)) {
            is ApiResult.Success -> parsing { parseRoute(r.value) }
            is ApiResult.Error -> r
        }
    }

    private fun parseRoute(o: JSONObject): RouteData {
        val line = o.getJSONArray("coordinates")
        val points = (0 until line.length()).map { parsePoint(line.getJSONObject(it)) }
        if (points.size < 2) throw JSONException("rota sem geometria")
        val stepsArr = o.optJSONArray("steps")
        val steps = if (stepsArr == null) emptyList() else (0 until stepsArr.length()).map { i ->
            val s = stepsArr.getJSONObject(i)
            RouteStepInfo(s.str("instruction"), s.str("streetName"), s.optDouble("distanceMeters", 0.0).roundToInt(), s.optJSONObject("location")?.let { parsePoint(it) })
        }
        return RouteData(
            points, o.getDouble("distanceMeters").roundToInt(), o.getDouble("durationSeconds").roundToInt(), steps,
            o.optBoolean("walking", false), o.str("warning"), o.str("safetyNotice"), o.str("attribution"),
        )
    }

    /** PCD: publica a própria posição. true = o backend guardou (há responsável autorizado); false = ninguém autorizado. */
    suspend fun publishLocation(token: String, point: LatLng, accuracyMeters: Float?): ApiResult<Boolean> {
        val body = JSONObject().put("latitude", point.latitude).put("longitude", point.longitude)
        if (accuracyMeters != null) body.put("precisaoMetros", accuracyMeters.toDouble())
        return when (val r = call("/api/pcd/localizacao", body, token)) {
            is ApiResult.Success -> ApiResult.Success(r.value.optBoolean("armazenada", false))
            is ApiResult.Error -> r
        }
    }

    /** Responsável: última posição de um PCD vinculado. Sucesso com null = autorizado, mas o aparelho ainda não enviou. */
    suspend fun pcdLocation(token: String, pcdId: Long): ApiResult<SharedLocation?> =
        when (val r = call("/api/responsavel/pcds/$pcdId/localizacao", null, token)) {
            is ApiResult.Success -> parsing { parseSharedLocation(r.value) }
            is ApiResult.Error -> r
        }

    private fun parseSharedLocation(o: JSONObject): SharedLocation? {
        if (!o.optBoolean("disponivel", false)) return null
        val d = o.optJSONObject("dados") ?: return null
        return SharedLocation(
            parsePoint(d), if (d.isNull("precisaoMetros")) null else d.optDouble("precisaoMetros"),
            d.str("atualizadoEm"), d.optLong("idadeSegundos", 0L),
        )
    }

    // ---- Câmera + IA ----

    /** Envia um quadro JPEG para o backend (que chama a IA YOLO) e recebe os objetos e as frases em português. */
    suspend fun detect(token: String, jpeg: ByteArray): ApiResult<DetectionResult> {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image", "frame.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()
        return when (val r = call("/detect", null, token, post = true, multipart = body)) {
            is ApiResult.Success -> {
                val o = r.value
                val arr = o.optJSONArray("objects")
                val items = if (arr == null) emptyList() else (0 until arr.length()).map { i ->
                    val x = arr.getJSONObject(i)
                    DetectedItem(
                        x.optString("class"), x.optString("label"), x.optDouble("confidence"),
                        x.optString("position"), x.optString("proximity"), x.optString("priority"),
                    )
                }
                val g = o.optJSONObject("guidance")
                ApiResult.Success(DetectionResult(items, g?.optString("message").orEmpty(), g?.optString("priority").orEmpty(), o.optString("scene")))
            }
            is ApiResult.Error -> r
        }
    }
}

/** Mensagem curta para a tela. Sem conexão (code -1) vira texto genérico; as demais já vêm em português do backend. */
fun ApiResult.Error.userMessage(): String =
    if (code == -1) "Sem conexão com o servidor. Verifique a internet e tente novamente." else message

/** Frase falada quando o servidor não respondeu. No build debug inclui o endereço usado, para facilitar o diagnóstico. */
fun ApiResult.Error.spokenFailure(): String =
    (if (BuildConfig.DEBUG) message else "Não consegui falar com o servidor.") + " Toque no botão para tentar de novo."
