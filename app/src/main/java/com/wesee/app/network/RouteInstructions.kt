package com.wesee.app.network

import com.wesee.app.settings.AppLanguage
import com.wesee.app.settings.AppSettings
import java.util.Locale

/**
 * Instrução de cada trecho da rota no idioma do app. Em PORTUGUÊS vale a frase que o backend já traduziu; em INGLÊS o app
 * traduz os dados da manobra que o OSRM devolveu (tipo, modificador, nome da via). Manobra desconhecida → null:
 * nada é inventado (a tela mostra só o nome da via e a distância).
 */
fun localizedInstruction(step: RouteStepInfo): String? =
    if (AppSettings.language == AppLanguage.PT) step.instruction?.takeIf { it.isNotBlank() } else englishInstruction(step)

private fun englishInstruction(step: RouteStepInfo): String? {
    val type = step.maneuverType?.lowercase(Locale.ROOT) ?: return null
    val mod = step.maneuverModifier?.lowercase(Locale.ROOT).orEmpty()
    val street = step.streetName?.trim().orEmpty()
    val onStreet = if (street.isEmpty()) "" else " onto $street"
    return when (type) {
        "depart" -> if (street.isEmpty()) "Start walking" else "Start on $street"
        "arrive" -> when (mod) {
            "left", "sharp left", "slight left" -> "You have arrived, on your left"
            "right", "sharp right", "slight right" -> "You have arrived, on your right"
            else -> "You have arrived at your destination"
        }
        "turn" -> action(mod)?.let { it.replaceFirstChar { c -> c.uppercase() } + onStreet }
        "end of road" -> action(mod)?.let { "At the end of the road, $it$onStreet" }
        "new name", "continue" -> {
            val a = if (mod.isEmpty() || mod == "straight") "continue straight" else action(mod)
            a?.replaceFirstChar { c -> c.uppercase() }?.plus(if (street.isEmpty()) "" else " on $street")
        }
        "fork" -> side(mod)?.let { "At the fork, keep $it" }
        "merge" -> "Merge"
        "roundabout", "rotary" -> "Enter the roundabout"
        "exit roundabout", "exit rotary" -> "Exit the roundabout"
        "roundabout turn" -> action(mod)?.let { it.replaceFirstChar { c -> c.uppercase() } + " at the roundabout" }
        else -> null
    }
}

private fun action(mod: String): String? = when (mod) {
    "left" -> "turn left"
    "right" -> "turn right"
    "sharp left" -> "turn sharp left"
    "sharp right" -> "turn sharp right"
    "slight left" -> "bear slightly left"
    "slight right" -> "bear slightly right"
    "straight" -> "continue straight"
    "uturn" -> "make a U-turn"
    else -> null
}

private fun side(mod: String): String? = when (mod) {
    "left", "sharp left", "slight left" -> "left"
    "right", "sharp right", "slight right" -> "right"
    "straight" -> "straight"
    else -> null
}
