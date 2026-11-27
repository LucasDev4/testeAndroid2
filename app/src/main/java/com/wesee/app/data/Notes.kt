package com.wesee.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.wesee.app.R
import com.wesee.app.settings.tr

/** [highlights]: trechos destacados como (início, fim exclusivo) em [text]. */
data class Note(val id: Long, val text: String, val color: Long, val highlights: List<Pair<Int, Int>> = emptyList())

val NoteColors: List<Long> = listOf(0xFF8CBBFF, 0xFFFFE680, 0xFFA8E6B0, 0xFFFFB3C7, 0xFFCDB8FF)

/** Anotações de exemplo (só aparecem enquanto o usuário não salvou nenhuma), no idioma atual do app. */
private fun seedNotes(): List<Note> {
    fun highlightOf(text: String, word: String): List<Pair<Int, Int>> {
        val i = text.indexOf(word)
        return if (i < 0) emptyList() else listOf(i to i + word.length)
    }
    val n1 = tr(R.string.seed_note_1)
    val n2 = tr(R.string.seed_note_2)
    return listOf(
        Note(1, n1, NoteColors[0], highlightOf(n1, tr(R.string.seed_note_1_hl))),
        Note(2, n2, NoteColors[1], highlightOf(n2, tr(R.string.seed_note_2_hl))),
        Note(3, tr(R.string.seed_note_3), NoteColors[2]),
    )
}

/** Persistência simples das anotações (SharedPreferences + JSON). */
class NotesStore(context: Context) {
    private val prefs = context.getSharedPreferences("wesee_notes", Context.MODE_PRIVATE)

    fun load(): List<Note> {
        val raw = prefs.getString(KEY, null) ?: return seedNotes()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val h = o.getJSONArray("h")
                Note(o.getLong("id"), o.getString("text"), o.getLong("color"),
                    (0 until h.length()).map { j -> h.getJSONArray(j).let { it.getInt(0) to it.getInt(1) } })
            }
        } catch (e: Exception) { seedNotes() }
    }

    fun save(notes: List<Note>) {
        val arr = JSONArray()
        notes.forEach { n ->
            val h = JSONArray()
            n.highlights.forEach { h.put(JSONArray().put(it.first).put(it.second)) }
            arr.put(JSONObject().put("id", n.id).put("text", n.text).put("color", n.color).put("h", h))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object { const val KEY = "notes" }
}
