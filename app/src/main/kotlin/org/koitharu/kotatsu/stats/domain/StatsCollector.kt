package org.koitharu.kotatsu.stats.domain

import androidx.collection.LongSparseArray
import androidx.collection.set
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.RetainedLifecycleCoroutineScope
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.stats.data.StatsEntity
import javax.inject.Inject

@ViewModelScoped
class StatsCollector @Inject constructor(
	private val db: MangaDatabase,
	private val settings: AppSettings,
	lifecycle: ViewModelLifecycle,
) {

	private val viewModelScope = RetainedLifecycleCoroutineScope(lifecycle)
	private val stats = LongSparseArray<Entry>(1)

	@Synchronized
	fun onStateChanged(mangaId: Long, state: ReaderState) {
		if (!settings.isStatsEnabled) {
			return
		}
		val now = System.currentTimeMillis()
		val entry = stats[mangaId]
		if (entry == null) {
			stats[mangaId] = Entry(
				state = state,
				stats = StatsEntity(
					mangaId = mangaId,
					startedAt = now,
					duration = 0,
					pages = 0,
				),
			)
			return
		}
		val pagesDelta = if (entry.state.page != state.page || entry.state.chapterId != state.chapterId) 1 else 0
		val newEntry = entry.copy(
			stats = StatsEntity(
				mangaId = mangaId,
				startedAt = entry.stats.startedAt,
				duration = now - entry.stats.startedAt,
				pages = entry.stats.pages + pagesDelta,
			),
		)
		stats[mangaId] = newEntry
		commit(newEntry.stats)
	}

	@Synchronized
	fun onPause(mangaId: Long) {
		stats.remove(mangaId)
	}

	private fun commit(entity: StatsEntity) {
		viewModelScope.launch(Dispatchers.Default) {
			runCatchingCancellable {
				db.getStatsDao().upsert(entity)
			}.onFailure { e ->
				e.printStackTraceDebug()
			}
		}
	}

	private data class Entry(
		val state: ReaderState,
		val stats: StatsEntity,
	)
}
