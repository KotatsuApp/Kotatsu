package org.koitharu.kotatsu.backups.data

import androidx.collection.ArrayMap
import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.backups.data.model.BackupIndex
import org.koitharu.kotatsu.backups.data.model.BookmarkBackup
import org.koitharu.kotatsu.backups.data.model.CategoryBackup
import org.koitharu.kotatsu.backups.data.model.FavouriteBackup
import org.koitharu.kotatsu.backups.data.model.HistoryBackup
import org.koitharu.kotatsu.backups.data.model.MangaBackup
import org.koitharu.kotatsu.backups.data.model.ScrobblingBackup
import org.koitharu.kotatsu.backups.data.model.SourceBackup
import org.koitharu.kotatsu.backups.data.model.StatisticBackup
import org.koitharu.kotatsu.backups.domain.BackupSection
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.CompositeResult
import org.koitharu.kotatsu.core.util.progress.Progress
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.filter.data.PersistableFilter
import org.koitharu.kotatsu.filter.data.SavedFiltersRepository
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.data.TapGridSettings
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@Reusable
class BackupRepository @Inject constructor(
    private val database: MangaDatabase,
    private val settings: AppSettings,
    private val tapGridSettings: TapGridSettings,
    private val mangaSourcesRepository: MangaSourcesRepository,
    private val savedFiltersRepository: SavedFiltersRepository,
) {

    private val json = Json {
        allowSpecialFloatingPointValues = true
        coerceInputValues = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        useAlternativeNames = false
    }

    suspend fun createBackup(
        output: ZipOutputStream,
        progress: FlowCollector<Progress>?,
    ) {
        progress?.emit(Progress.INDETERMINATE)
        var commonProgress = Progress(0, BackupSection.entries.size)
        for (section in BackupSection.entries) {
            when (section) {
                BackupSection.INDEX -> output.writeJsonArray(
                    section = BackupSection.INDEX,
                    data = flowOf(BackupIndex()),
                    serializer = serializer(),
                )

                BackupSection.HISTORY -> output.writeJsonArray(
                    section = BackupSection.HISTORY,
                    data = database.getHistoryDao().dump().map { HistoryBackup(it) },
                    serializer = serializer(),
                )

                BackupSection.CATEGORIES -> output.writeJsonArray(
                    section = BackupSection.CATEGORIES,
                    data = database.getFavouriteCategoriesDao().findAll().asFlow().map { CategoryBackup(it) },
                    serializer = serializer(),
                )

                BackupSection.FAVOURITES -> output.writeJsonArray(
                    section = BackupSection.FAVOURITES,
                    data = database.getFavouritesDao().dump().map { FavouriteBackup(it) },
                    serializer = serializer(),
                )

                BackupSection.SETTINGS -> output.writeString(
                    section = BackupSection.SETTINGS,
                    data = dumpSettings(),
                )

                BackupSection.SETTINGS_READER_GRID -> output.writeString(
                    section = BackupSection.SETTINGS_READER_GRID,
                    data = dumpReaderGridSettings(),
                )

                BackupSection.BOOKMARKS -> output.writeJsonArray(
                    section = BackupSection.BOOKMARKS,
                    data = database.getBookmarksDao().dump().map { BookmarkBackup(it.first, it.second) },
                    serializer = serializer(),
                )

                BackupSection.SOURCES -> output.writeJsonArray(
                    section = BackupSection.SOURCES,
                    data = database.getSourcesDao().dumpEnabled().map { SourceBackup(it) },
                    serializer = serializer(),
                )

                BackupSection.SCROBBLING -> output.writeJsonArray(
                    section = BackupSection.SCROBBLING,
                    data = database.getScrobblingDao().dumpEnabled().map { ScrobblingBackup(it) },
                    serializer = serializer(),
                )

                BackupSection.STATS -> output.writeJsonArray(
                    section = BackupSection.STATS,
                    data = database.getStatsDao().dumpEnabled().map { StatisticBackup(it) },
                    serializer = serializer(),
                )

                BackupSection.SAVED_FILTERS -> {
                    val sources = mangaSourcesRepository.getEnabledSources()
                    val filters = sources.flatMap { source ->
                        savedFiltersRepository.getAll(source)
                    }
                    output.writeJsonArray(
                        section = BackupSection.SAVED_FILTERS,
                        data = filters.asFlow(),
                        serializer = serializer(),
                    )
                }
            }
            progress?.emit(commonProgress)
            commonProgress++
        }
        progress?.emit(commonProgress)
    }

    suspend fun restoreBackup(
        input: ZipInputStream,
        sections: Set<BackupSection>,
        progress: FlowCollector<Progress>?,
    ): CompositeResult {
        progress?.emit(Progress.INDETERMINATE)
        var commonProgress = Progress(0, sections.size)
        var entry = input.nextEntry
        var result = CompositeResult.EMPTY
        while (entry != null) {
            val section = BackupSection.of(entry)
            if (section in sections) {
                result += when (section) {
                    BackupSection.INDEX -> CompositeResult.EMPTY // useless in our case
                    BackupSection.HISTORY -> input.readJsonArray<HistoryBackup>(serializer()).restoreToDb {
                        upsertManga(it.manga)
                        getHistoryDao().upsert(it.toEntity())
                    }

                    BackupSection.CATEGORIES -> input.readJsonArray<CategoryBackup>(serializer()).restoreToDb {
                        getFavouriteCategoriesDao().upsert(it.toEntity())
                    }

                    BackupSection.FAVOURITES -> input.readJsonArray<FavouriteBackup>(serializer()).restoreToDb {
                        upsertManga(it.manga)
                        getFavouritesDao().upsert(it.toEntity())
                    }

                    BackupSection.SETTINGS -> input.readMap().let {
                        settings.upsertAll(it)
                        CompositeResult.success()
                    }

                    BackupSection.SETTINGS_READER_GRID -> input.readMap().let {
                        tapGridSettings.upsertAll(it)
                        CompositeResult.success()
                    }

                    BackupSection.BOOKMARKS -> input.readJsonArray<BookmarkBackup>(serializer()).restoreToDb {
                        upsertManga(it.manga)
                        getBookmarksDao().upsert(it.bookmarks.map { b -> b.toEntity() })
                    }

                    BackupSection.SOURCES -> input.readJsonArray<SourceBackup>(serializer()).restoreToDb {
                        getSourcesDao().upsert(it.toEntity())
                    }

                    BackupSection.SCROBBLING -> input.readJsonArray<ScrobblingBackup>(serializer()).restoreToDb {
                        getScrobblingDao().upsert(it.toEntity())
                    }

                    BackupSection.STATS -> input.readJsonArray<StatisticBackup>(serializer()).restoreToDb {
                        getStatsDao().upsert(it.toEntity())
                    }

                    BackupSection.SAVED_FILTERS -> input.readJsonArray<PersistableFilter>(serializer())
                        .restoreWithoutTransaction {
                            savedFiltersRepository.save(it)
                        }

                    null -> CompositeResult.EMPTY // skip unknown entries
                }
                progress?.emit(commonProgress)
                commonProgress++
            }
            input.closeEntry()
            entry = input.nextEntry
        }
        progress?.emit(commonProgress)
        return result
    }

    private suspend fun <T> ZipOutputStream.writeJsonArray(
        section: BackupSection,
        data: Flow<T>,
        serializer: SerializationStrategy<T>,
    ) {
        data.onStart {
            putNextEntry(ZipEntry(section.entryName))
            write("[")
        }.onCompletion { error ->
            if (error == null) {
                write("]")
            }
            closeEntry()
            flush()
        }.collectIndexed { index, value ->
            if (index > 0) {
                write(",")
            }
            json.encodeToStream(serializer, value, this)
        }
    }

    private fun <T> InputStream.readJsonArray(
        serializer: DeserializationStrategy<T>,
    ): Sequence<T> = json.decodeToSequence(this, serializer, DecodeSequenceMode.ARRAY_WRAPPED)

    private fun InputStream.readMap(): Map<String, Any?> {
        val jo = JSONArray(readString()).getJSONObject(0)
        val map = ArrayMap<String, Any?>(jo.length())
        val keys = jo.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jo.get(key)
        }
        return map
    }

    private fun ZipOutputStream.writeString(
        section: BackupSection,
        data: String,
    ) {
        putNextEntry(ZipEntry(section.entryName))
        try {
            write("[")
            write(data)
            write("]")
        } finally {
            closeEntry()
            flush()
        }
    }

    private fun OutputStream.write(str: String) = write(str.toByteArray())

    private fun InputStream.readString(): String = readBytes().decodeToString()

    private fun dumpSettings(): String {
        val map = settings.getAllValues().toMutableMap()
        map.remove(AppSettings.KEY_APP_PASSWORD)
        map.remove(AppSettings.KEY_PROXY_PASSWORD)
        map.remove(AppSettings.KEY_PROXY_LOGIN)
        map.remove(AppSettings.KEY_INCOGNITO_MODE)
        return JSONObject(map).toString()
    }

    private fun dumpReaderGridSettings(): String {
        return JSONObject(tapGridSettings.getAllValues()).toString()
    }

    private suspend fun MangaDatabase.upsertManga(manga: MangaBackup) {
        val tags = manga.tags.map { it.toEntity() }
        getTagsDao().upsert(tags)
        getMangaDao().upsert(manga.toEntity(), tags)
    }

    private suspend inline fun <T> Sequence<T>.restoreToDb(crossinline block: suspend MangaDatabase.(T) -> Unit): CompositeResult {
        return fold(CompositeResult.EMPTY) { result, item ->
            result + runCatchingCancellable {
                database.withTransaction {
                    database.block(item)
                }
            }
        }
    }

    private suspend inline fun <T> Sequence<T>.restoreWithoutTransaction(crossinline block: suspend (T) -> Unit): CompositeResult {
        return fold(CompositeResult.EMPTY) { result, item ->
            result + runCatchingCancellable {
                block(item)
            }
        }
    }
}
