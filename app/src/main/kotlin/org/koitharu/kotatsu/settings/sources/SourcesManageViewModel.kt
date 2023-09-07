package org.koitharu.kotatsu.settings.sources

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.removeObserverAsync
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.move
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@HiltViewModel
class SourcesManageViewModel @Inject constructor(
	private val database: MangaDatabase,
	private val settings: AppSettings,
	private val repository: MangaSourcesRepository,
	private val listProducer: SourcesListProducer,
) : BaseViewModel() {

	val content = listProducer.list
	val onActionDone = MutableEventFlow<ReversibleAction>()
	private var commitJob: Job? = null

	init {
		launchJob(Dispatchers.Default) {
			database.invalidationTracker.addObserver(listProducer)
		}
	}

	override fun onCleared() {
		super.onCleared()
		database.invalidationTracker.removeObserverAsync(listProducer)
	}

	fun reorderSources(oldPos: Int, newPos: Int) {
		val snapshot = content.value.toMutableList()
		if ((snapshot[oldPos] as? SourceConfigItem.SourceItem)?.isDraggable != true) {
			return
		}
		if ((snapshot[newPos] as? SourceConfigItem.SourceItem)?.isDraggable != true) {
			return
		}
		snapshot.move(oldPos, newPos)
		content.value = snapshot
		commit(snapshot)
	}

	fun canReorder(oldPos: Int, newPos: Int): Boolean {
		val snapshot = content.value
		if ((snapshot[oldPos] as? SourceConfigItem.SourceItem)?.isEnabled != true) return false
		return (snapshot[newPos] as? SourceConfigItem.SourceItem)?.isEnabled == true
	}

	fun setEnabled(source: MangaSource, isEnabled: Boolean) {
		launchJob(Dispatchers.Default) {
			val rollback = repository.setSourceEnabled(source, isEnabled)
			if (!isEnabled) {
				onActionDone.call(ReversibleAction(R.string.source_disabled, rollback))
			}
		}
	}

	fun bringToTop(source: MangaSource) {
		var oldPos = -1
		var newPos = -1
		val snapshot = content.value
		for ((i, x) in snapshot.withIndex()) {
			if (x !is SourceConfigItem.SourceItem) {
				continue
			}
			if (newPos == -1) {
				newPos = i
			}
			if (x.source == source) {
				oldPos = i
				break
			}
		}
		@Suppress("KotlinConstantConditions")
		if (oldPos != -1 && newPos != -1) {
			reorderSources(oldPos, newPos)
			val revert = ReversibleAction(R.string.moved_to_top) {
				reorderSources(newPos, oldPos)
			}
			onActionDone.call(revert)
		}
	}

	fun disableAll() {
		launchJob(Dispatchers.Default) {
			repository.disableAllSources()
		}
	}

	fun expandOrCollapse(headerId: String?) {
		listProducer.expandCollapse(headerId)
	}

	fun performSearch(query: String?) {
		listProducer.setQuery(query?.trim().orEmpty())
	}

	fun onTipClosed(item: SourceConfigItem.Tip) {
		launchJob(Dispatchers.Default) {
			settings.closeTip(item.key)
		}
	}

	private fun commit(snapshot: List<SourceConfigItem>) {
		val prevJob = commitJob
		commitJob = launchJob {
			prevJob?.cancelAndJoin()
			delay(500)
			val newSourcesList = snapshot.mapNotNull { x ->
				if (x is SourceConfigItem.SourceItem && x.isDraggable) {
					x.source
				} else {
					null
				}
			}
			repository.setPositions(newSourcesList)
			yield()
		}
	}
}
