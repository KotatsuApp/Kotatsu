package org.koitharu.kotatsu.local.data.index

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class LocalMangaIndex @Inject constructor(
	private val mangaDataRepository: MangaDataRepository,
	private val db: MangaDatabase,
	private val localStorageManager: LocalStorageManager,
	@ApplicationContext context: Context,
	private val localMangaRepositoryProvider: Provider<LocalMangaRepository>,
) : FlowCollector<LocalManga?> {

	private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	private var previousHash: Long
		get() = prefs.getLong(KEY_HASH, 0L)
		set(value) = prefs.edit { putLong(KEY_HASH, value) }

	override suspend fun emit(value: LocalManga?) {
		if (value != null) {
			put(value)
		}
	}

	suspend fun update(): Boolean {
		val newHash = computeHash()
		if (newHash == previousHash) {
			return false
		}
		db.withTransaction {
			val dao = db.getLocalMangaIndexDao()
			dao.clear()
			localMangaRepositoryProvider.get().getRawListAsFlow()
				.collect { dao.upsert(it.toEntity()) }
		}
		previousHash = newHash
		return true
	}

	suspend fun get(mangaId: Long): LocalManga? {
		val path = db.getLocalMangaIndexDao().findPath(mangaId) ?: return null
		return runCatchingCancellable {
			LocalMangaInput.of(File(path)).getManga()
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	suspend fun put(manga: LocalManga) = db.withTransaction {
		mangaDataRepository.storeManga(manga.manga)
		db.getLocalMangaIndexDao().upsert(manga.toEntity())
	}

	suspend fun delete(mangaId: Long) {
		db.getLocalMangaIndexDao().delete(mangaId)
	}

	private fun LocalManga.toEntity() = LocalMangaIndexEntity(
		mangaId = manga.id,
		path = file.path,
	)

	private suspend fun computeHash(): Long {
		return runCatchingCancellable {
			localStorageManager.getReadableDirs()
				.fold(0L) { acc, file -> acc + file.computeHash() }
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrDefault(0L)
	}

	private suspend fun File.computeHash(): Long = runInterruptible(Dispatchers.IO) {
		lastModified() // TODO size
	}

	companion object {

		private const val PREF_NAME = "_local_index"
		private const val KEY_HASH = "hash"
	}
}
