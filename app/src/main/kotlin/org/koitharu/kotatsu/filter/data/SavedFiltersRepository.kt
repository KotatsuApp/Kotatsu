package org.koitharu.kotatsu.filter.data

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Demographic
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.MangaState
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

@Singleton
class SavedFiltersRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val keyRoot = "saved_filters_v1"

    private val state = MutableStateFlow<Map<String, List<Preset>>>(emptyMap())

    init {
        scope.launch { loadAll() }
    }

    data class Preset(
        val id: Long,
        val name: String,
        val source: String,
        val payload: JSONObject,
    )

    fun observe(source: String): StateFlow<List<Preset>> = MutableStateFlow(state.value[source].orEmpty()).also { out ->
        scope.launch {
            state.collect { all -> out.value = all[source].orEmpty() }
        }
    }

    fun list(source: String): List<Preset> = state.value[source].orEmpty()

    fun save(source: String, name: String, filter: MangaListFilter): Preset {
        val nowId = System.currentTimeMillis()
        val preset = Preset(
            id = nowId,
            name = name,
            source = source,
            payload = serializeFilter(filter),
        )
        val list = list(source) + preset
        persist(source, list)
        return preset
    }

    fun rename(source: String, id: Long, newName: String) {
        val list = list(source).map { if (it.id == id) it.copy(name = newName) else it }
        persist(source, list)
    }

    fun delete(source: String, id: Long) {
        val list = list(source).filterNot { it.id == id }
        persist(source, list)
    }

    private fun persist(source: String, list: List<Preset>) {
        val root = JSONObject(prefs.getString(keyRoot, "{}"))
        root.put(source, JSONArray(list.map { presetToJson(it) }))
        prefs.edit { putString(keyRoot, root.toString()) }
        state.value = state.value.toMutableMap().also { it[source] = list }
    }

    private fun loadAll() {
        val root = JSONObject(prefs.getString(keyRoot, "{}"))
        val map = mutableMapOf<String, List<Preset>>()
        for (key in root.keys()) {
            val arr = root.optJSONArray(key) ?: continue
            map[key] = (0 until arr.length()).mapNotNull { i -> jsonToPreset(arr.optJSONObject(i), key) }
        }
        state.value = map
    }

    private fun presetToJson(p: Preset): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("payload", p.payload)
    }

    private fun jsonToPreset(obj: JSONObject?, source: String): Preset? {
        obj ?: return null
        val id = obj.optLong("id", 0L)
        val name = obj.optString("name", null) ?: return null
        val payload = obj.optJSONObject("payload") ?: return null
        return Preset(id, name, source, payload)
    }

    fun serializeFilter(f: MangaListFilter): JSONObject = JSONObject().apply {
        put("query", f.query)
        put("author", f.author)
        put("locale", f.locale?.toLanguageTag())
        put("originalLocale", f.originalLocale?.toLanguageTag())
        put("states", JSONArray(f.states.map { it.name }))
        put("contentRating", JSONArray(f.contentRating.map { it.name }))
        put("types", JSONArray(f.types.map { it.name }))
        put("demographics", JSONArray(f.demographics.map { it.name }))
        put("tags", JSONArray(f.tags.map { it.key }))
        put("tagsExclude", JSONArray(f.tagsExclude.map { it.key }))
        put("year", f.year)
        put("yearFrom", f.yearFrom)
        put("yearTo", f.yearTo)
    }

    fun deserializeFilter(
        obj: JSONObject,
        resolveTags: (Set<String>) -> Set<MangaTag>,
    ): MangaListFilter {
        return MangaListFilter(
            query = obj.optString("query").takeIf { it.isNotEmpty() },
            author = obj.optString("author").takeIf { it.isNotEmpty() },
            locale = obj.optString("locale").takeIf { it.isNotEmpty() }?.let { Locale.forLanguageTag(it) },
            originalLocale = obj.optString("originalLocale").takeIf { it.isNotEmpty() }?.let { Locale.forLanguageTag(it) },
            states = obj.optJSONArray("states")?.toStringSet()?.mapNotNull { runCatching { MangaState.valueOf(it) }.getOrNull() }?.toSet().orEmpty(),
            contentRating = obj.optJSONArray("contentRating")?.toStringSet()?.mapNotNull { runCatching { ContentRating.valueOf(it) }.getOrNull() }?.toSet().orEmpty(),
            types = obj.optJSONArray("types")?.toStringSet()?.mapNotNull { runCatching { ContentType.valueOf(it) }.getOrNull() }?.toSet().orEmpty(),
            demographics = obj.optJSONArray("demographics")?.toStringSet()?.mapNotNull { runCatching { Demographic.valueOf(it) }.getOrNull() }?.toSet().orEmpty(),
            tags = resolveTags(obj.optJSONArray("tags")?.toStringSet().orEmpty()).toSet(),
            tagsExclude = resolveTags(obj.optJSONArray("tagsExclude")?.toStringSet().orEmpty()).toSet(),
            year = obj.optInt("year"),
            yearFrom = obj.optInt("yearFrom"),
            yearTo = obj.optInt("yearTo"),
        )
    }
}

private fun JSONArray.toStringSet(): Set<String> = buildSet {
    for (i in 0 until length()) {
        val v = optString(i)
        if (!v.isNullOrEmpty()) add(v)
    }
}
