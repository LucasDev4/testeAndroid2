package com.wesee.app.ui.screens.pcd.home.camera

import com.wesee.app.network.DetectedItem
import com.wesee.app.network.DetectionResult
import com.wesee.app.settings.AppLanguage
import com.wesee.app.settings.AppSettings
import java.util.Locale

/**
 * Texto da cena/alerta no idioma do app. Em PORTUGUÊS vale o texto que o backend já devolve. Em INGLÊS o app monta a frase
 * a partir dos objetos detectados (classe, posição, proximidade, prioridade), seguindo a mesma lógica do backend
 * (GuidanceService): assim a IA e o backend não precisam mudar e a voz inglesa não lê texto em português.
 */
object SceneDescriber {

    private class Noun(val singular: String, val plural: String, val context: Boolean = false)

    /** Classes do modelo (nomes em inglês) e alguns apelidos em português que o backend também aceita. */
    private val nouns: Map<String, Noun> = buildMap {
        fun n(vararg keys: String, noun: Noun) = keys.forEach { put(it, noun) }
        n("person", "pedestrian", "pessoa", noun = Noun("person", "people"))
        n("vehicle", "veículo", "veiculo", noun = Noun("vehicle", "vehicles"))
        n("car", "carro", noun = Noun("car", "cars"))
        n("bus", "ônibus", "onibus", noun = Noun("bus", "buses"))
        n("truck", "caminhão", "caminhao", noun = Noun("truck", "trucks"))
        n("motorcycle", "moto", "motocicleta", noun = Noun("motorcycle", "motorcycles"))
        n("bicycle", "bicicleta", noun = Noun("bicycle", "bicycles"))
        n("stairs", "escada", "escadas", noun = Noun("staircase", "staircases"))
        n("manhole", "bueiro", noun = Noun("manhole", "manholes"))
        n("dog", "cachorro", noun = Noun("dog", "dogs"))
        n("electrical pole", "poste", noun = Noun("pole", "poles"))
        n("dustbin", "lixeira", noun = Noun("trash bin", "trash bins"))
        n("traffic cone", "cone", noun = Noun("traffic cone", "traffic cones"))
        n("traffic barrel", noun = Noun("traffic barrel", "traffic barrels"))
        n("fire hydrant", "hidrante", noun = Noun("fire hydrant", "fire hydrants"))
        n("guard rail", "grade", noun = Noun("guard rail", "guard rails"))
        n("door", "porta", noun = Noun("door", "doors"))
        n("obstacle", "obstáculo", "obstaculo", noun = Noun("obstacle", "obstacles"))
        n("tree", "árvore", "arvore", noun = Noun("tree", "trees"))
        n("bench", "banco", noun = Noun("bench", "benches"))
        n("chair", "cadeira", noun = Noun("chair", "chairs"))
        n("plant pot", "vaso", noun = Noun("plant pot", "plant pots"))
        n("electrical box", "caixa elétrica", noun = Noun("electrical box", "electrical boxes"))
        n("bicycle rack", "bicicletário", noun = Noun("bicycle rack", "bicycle racks"))
        n("traffic sign", "placa", noun = Noun("traffic sign", "traffic signs"))
        n("pedestrian crosswalk", "faixa de pedestres", noun = Noun("crosswalk", "crosswalks"))
        // contexto (descrevem o ambiente; não geram alerta)
        n("road", "rua", noun = Noun("road", "roads", context = true))
        n("building", "prédio", "predio", noun = Noun("building", "buildings", context = true))
    }

    private fun nounOf(item: DetectedItem): Noun {
        val key = item.className.trim().lowercase(Locale.ROOT)
        return nouns[key] ?: Noun(key.ifBlank { "object" }, (key.ifBlank { "object" }) + "s")
    }

    private fun rank(p: String): Int = when (p) { "HIGH" -> 2; "MEDIUM" -> 1; else -> 0 }
    private fun proximityRank(p: String): Int = when (p) { "NEAR" -> 2; "MEDIUM" -> 1; else -> 0 }

    private fun direction(position: String): String = when (position.uppercase(Locale.ROOT)) {
        "LEFT" -> "to your left"
        "RIGHT" -> "to your right"
        "CENTER" -> "ahead of you"
        else -> "nearby"
    }

    /** Descrição da cena para ser exibida e falada. */
    fun scene(r: DetectionResult): String =
        if (AppSettings.language == AppLanguage.PT) r.scene else englishScene(r.objects)

    /** Alerta curto do objeto mais urgente (sem o "Atenção!"). */
    fun alert(r: DetectionResult): String =
        if (AppSettings.language == AppLanguage.PT) r.alert else englishAlert(r.objects)

    private fun englishScene(objects: List<DetectedItem>): String {
        if (objects.isEmpty()) return "Path clear, no objects in sight."
        data class Group(val noun: Noun, val position: String, val count: Int, val priority: Int, val near: Boolean)
        val groups = objects.groupBy { nounOf(it).singular to it.position }.map { (_, items) ->
            val first = items.first()
            Group(
                nounOf(first), first.position, items.size,
                items.maxOf { rank(it.priority) }, items.any { it.proximity == "NEAR" },
            )
        }.sortedWith(compareByDescending<Group> { it.priority }.thenByDescending { it.near }.thenByDescending { it.count }).take(5)
        val phrases = groups.map { g ->
            val quantity = when (g.count) { 1 -> "one"; 2 -> "two"; else -> "several" }
            val noun = if (g.count == 1) g.noun.singular else g.noun.plural
            val close = if (g.near && !g.noun.context) " very close" else ""
            "$quantity $noun$close ${direction(g.position)}"
        }
        val joined = if (phrases.size <= 1) phrases.joinToString("") else phrases.dropLast(1).joinToString(", ") + " and " + phrases.last()
        return "I see $joined."
    }

    private fun englishAlert(objects: List<DetectedItem>): String {
        val relevant = objects.filter { !nounOf(it).context }
        val top = relevant.sortedWith(
            compareByDescending<DetectedItem> { rank(it.priority) }
                .thenByDescending { proximityRank(it.proximity) }
                .thenByDescending { it.confidence },
        ).firstOrNull() ?: return ""
        val name = nounOf(top).singular.replaceFirstChar { it.uppercase() }
        val close = if (top.proximity == "NEAR") " very close" else ""
        return "$name$close ${direction(top.position)}"
    }
}
