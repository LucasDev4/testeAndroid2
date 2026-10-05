package com.wesee.app.data

import android.content.Context

/** Perfil local do usuário (nome/e-mail do cadastro). Será substituído pelo backend futuramente. */
class UserStore(context: Context) {
    private val prefs = context.getSharedPreferences("wesee_user", Context.MODE_PRIVATE)
    var name: String
        get() = prefs.getString("name", "") ?: ""
        private set(value) { prefs.edit().putString("name", value).apply() }

    fun saveProfile(name: String, email: String) {
        this.name = name
        prefs.edit().putString("email", email).apply()
    }

    val firstName: String get() = name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "usuário" }
}
