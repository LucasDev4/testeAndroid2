package com.wesee.app.data

import android.content.Context
import android.net.Uri
import java.io.File

/** Perfil + sessão local do usuário. Será substituído pelo backend futuramente. */
class UserStore(context: Context) {
    private val prefs = context.getSharedPreferences("wesee_user", Context.MODE_PRIVATE)

    val name: String get() = prefs.getString("name", "") ?: ""
    val firstName: String get() = name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "usuário" }
    /** Inicial usada no avatar quando não há foto. */
    val initial: String get() = name.trim().firstOrNull()?.uppercase() ?: "U"

    fun saveProfile(name: String, email: String) {
        prefs.edit().putString("name", name).putString("email", email).apply()
    }

    /** Sessão: true após login (voz/biometria); logout invalida. */
    var isLoggedIn: Boolean
        get() = prefs.getBoolean("logged_in", false)
        set(value) { prefs.edit().putBoolean("logged_in", value).apply() }

    fun logout() { isLoggedIn = false }

    var avatarPath: String?
        get() = prefs.getString("avatar", null)?.takeIf { File(it).exists() }
        private set(value) { prefs.edit().putString("avatar", value).apply() }

    /** Copia a imagem escolhida para o armazenamento do app e a define como avatar. Chamar fora da thread principal. */
    fun saveAvatar(context: Context, uri: Uri): String? = try {
        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return null
        avatarPath?.let { File(it).delete() }
        avatarPath = file.absolutePath
        file.absolutePath
    } catch (e: Exception) { null }
}
