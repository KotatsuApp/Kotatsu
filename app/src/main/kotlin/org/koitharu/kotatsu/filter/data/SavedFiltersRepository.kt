package org.koitharu.kotatsu.filter.data

import android.content.Context
import androidx.core.content.edit
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koitharu.kotatsu.core.util.ext.observeChanges
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject

@Reusable
class SavedFiltersRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun observeAll(source: MangaSource): Flow<List<PersistableFilter>> = getPrefs(source).observeChanges()
        .onStart { emit(null) }
        .map {
            getAll(source)
        }.distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    suspend fun getAll(source: MangaSource): List<PersistableFilter> = withContext(Dispatchers.Default) {
        val prefs = getPrefs(source)
        val keys = prefs.all.keys.filter { it.startsWith(FILTER_PREFIX) }
        keys.mapNotNull { key ->
            val value = prefs.getString(key, null) ?: return@mapNotNull null
            try {
                Json.decodeFromString(value)
            } catch (e: SerializationException) {
                e.printStackTraceDebug()
                null
            }
        }
    }

    suspend fun save(
        source: MangaSource,
        name: String,
        filter: MangaListFilter,
    ): PersistableFilter = withContext(Dispatchers.Default) {
        val persistableFilter = PersistableFilter(
            name = name,
            source = source,
            filter = filter,
        )
        persist(source, persistableFilter)
        persistableFilter
    }

    suspend fun rename(source: MangaSource, id: Int, newName: String) = withContext(Dispatchers.Default) {
        val filter = load(source, id) ?: return@withContext
        persist(source, filter.copy(name = newName))
    }

    suspend fun delete(source: MangaSource, id: Int) = withContext(Dispatchers.Default) {
        val prefs = getPrefs(source)
        prefs.edit(commit = true) {
            remove(FILTER_PREFIX + id)
        }
    }

    private fun persist(source: MangaSource, persistableFilter: PersistableFilter) {
        val prefs = getPrefs(source)
        val json = Json.encodeToString(persistableFilter)
        prefs.edit(commit = true) {
            putString(FILTER_PREFIX + persistableFilter.id, json)
        }
    }

    private fun load(source: MangaSource, id: Int): PersistableFilter? {
        val prefs = getPrefs(source)
        val json = prefs.getString(FILTER_PREFIX + id, null) ?: return null
        return try {
            Json.decodeFromString<PersistableFilter>(json)
        } catch (e: SerializationException) {
            e.printStackTraceDebug()
            null
        }
    }

    private fun getPrefs(source: MangaSource) = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

    private companion object {

        const val FILTER_PREFIX = "__pf_"
    }
}
