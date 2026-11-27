package com.wesee.app.settings

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Idiomas do app. [tag] é o valor salvo; [locale] é usado em textos, fala (TTS) e reconhecimento de voz. */
enum class AppLanguage(val tag: String, val locale: Locale, val shortLabel: String) {
    PT("pt-BR", Locale("pt", "BR"), "PT - BR"),
    EN("en", Locale.US, "EN");

    companion object {
        fun fromTag(tag: String?): AppLanguage = values().firstOrNull { it.tag == tag } ?: PT
    }
}

/**
 * Preferências do APP (não do aparelho): idioma, tema e notificações. Ficam em um arquivo próprio ("wesee_settings"),
 * que NÃO é apagado no logout. Os valores são estados do Compose: ao mudar, todas as telas que os leem recompõem na hora.
 * Padrões: Português (Brasil), tema claro, notificações ativadas.
 */
object AppSettings {
    private const val FILE = "wesee_settings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_DARK = "dark_theme"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    var language by mutableStateOf(AppLanguage.PT)
        private set
    var darkTheme by mutableStateOf(false)
        private set
    var notificationsEnabled by mutableStateOf(true)
        private set

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private val resourcesCache = HashMap<AppLanguage, Resources>()

    /** Carrega as preferências salvas. Chamar uma vez, antes de desenhar a tela (MainActivity.onCreate). */
    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        appContext = app
        val p = app.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        language = AppLanguage.fromTag(p.getString(KEY_LANGUAGE, null))
        darkTheme = p.getBoolean(KEY_DARK, false)
        notificationsEnabled = p.getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setLanguage(value: AppLanguage) {
        language = value
        prefs?.edit()?.putString(KEY_LANGUAGE, value.tag)?.apply()
    }

    fun setDarkTheme(value: Boolean) {
        darkTheme = value
        prefs?.edit()?.putBoolean(KEY_DARK, value)?.apply()
    }

    /** Só guarda a preferência: o app ainda NÃO envia notificações reais (e por isso não pede a permissão de notificações). */
    fun setNotificationsEnabled(value: Boolean) {
        notificationsEnabled = value
        prefs?.edit()?.putBoolean(KEY_NOTIFICATIONS, value)?.apply()
    }

    /**
     * Texto no idioma escolhido para código FORA de composables (voz, mensagens de estado, erros de rede...).
     * Lê [language]: dentro de um composable, a leitura faz a tela recompor quando o idioma muda.
     */
    fun string(@StringRes id: Int, vararg args: Any): String {
        val res = resourcesFor(language)
        return if (args.isEmpty()) res.getString(id) else res.getString(id, *args)
    }

    private fun resourcesFor(lang: AppLanguage): Resources {
        resourcesCache[lang]?.let { return it }
        val app = appContext ?: error("AppSettings.init(context) não foi chamado")
        val cfg = Configuration(app.resources.configuration).apply { setLocale(lang.locale) }
        return app.createConfigurationContext(cfg).resources.also { resourcesCache[lang] = it }
    }

    /**
     * Contexto cujos recursos (strings.xml / values-en) seguem o idioma do app, SEM mexer no idioma do aparelho.
     * É um ContextWrapper: continua sendo possível chegar à Activity (findActivity()).
     */
    fun localizedContext(base: Context, lang: AppLanguage): Context {
        val cfg = Configuration(base.resources.configuration).apply { setLocale(lang.locale) }
        val localizedResources = base.createConfigurationContext(cfg).resources
        return object : ContextWrapper(base) {
            override fun getResources(): Resources = localizedResources
        }
    }
}

/** Atalho: texto no idioma do app para código que não é composable. */
fun tr(@StringRes id: Int, vararg args: Any): String = AppSettings.string(id, *args)
