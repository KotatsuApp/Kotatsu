package org.koitharu.kotatsu.download.ui.list

import androidx.collection.LongSparseArray
import androidx.collection.getOrElse
import androidx.collection.set
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.WorkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.daysDiff
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.Date
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
	private val workScheduler: DownloadWorker.Scheduler,
	private val mangaDataRepository: MangaDataRepository,
) : BaseViewModel() {

	private val mangaCache = LongSparseArray<Manga>()
	private val cacheMutex = Mutex()
	private val works = workScheduler.observeWorks()
		.mapLatest { it.toDownloadsList() }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val onActionDone = MutableEventFlow<ReversibleAction>()

	val items = works.map {
		it?.toUiList() ?: listOf(LoadingState)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	val hasPausedWorks = works.map {
		it?.any { x -> x.canResume } == true
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), false)

	val hasActiveWorks = works.map {
		it?.any { x -> x.canPause } == true
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), false)

	val hasCancellableWorks = works.map {
		it?.any { x -> !x.workState.isFinished } == true
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), false)

	fun cancel(id: UUID) {
		launchJob(Dispatchers.Default) {
			workScheduler.cancel(id)
		}
	}

	fun cancel(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			val snapshot = works.value ?: return@launchJob
			for (work in snapshot) {
				if (work.id.mostSignificantBits in ids) {
					workScheduler.cancel(work.id)
				}
			}
			onActionDone.call(ReversibleAction(R.string.downloads_cancelled, null))
		}
	}

	fun cancelAll() {
		launchJob(Dispatchers.Default) {
			workScheduler.cancelAll()
			onActionDone.call(ReversibleAction(R.string.downloads_cancelled, null))
		}
	}

	fun pause(ids: Set<Long>) {
		val snapshot = works.value ?: return
		for (work in snapshot) {
			if (work.id.mostSignificantBits in ids) {
				workScheduler.pause(work.id)
			}
		}
		onActionDone.call(ReversibleAction(R.string.downloads_paused, null))
	}

	fun pauseAll() {
		val snapshot = works.value ?: return
		var isPaused = false
		for (work in snapshot) {
			if (work.canPause) {
				workScheduler.pause(work.id)
				isPaused = true
			}
		}
		if (isPaused) {
			onActionDone.call(ReversibleAction(R.string.downloads_paused, null))
		}
	}

	fun resumeAll() {
		val snapshot = works.value ?: return
		var isResumed = false
		for (work in snapshot) {
			if (work.workState == WorkInfo.State.RUNNING && work.isPaused) {
				workScheduler.resume(work.id)
				isResumed = true
			}
		}
		if (isResumed) {
			onActionDone.call(ReversibleAction(R.string.downloads_resumed, null))
		}
	}

	fun resume(ids: Set<Long>) {
		val snapshot = works.value ?: return
		for (work in snapshot) {
			if (work.id.mostSignificantBits in ids) {
				workScheduler.resume(work.id)
			}
		}
		onActionDone.call(ReversibleAction(R.string.downloads_resumed, null))
	}

	fun remove(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			val snapshot = works.value ?: return@launchJob
			val uuids = HashSet<UUID>(ids.size)
			for (work in snapshot) {
				if (work.id.mostSignificantBits in ids) {
					uuids.add(work.id)
				}
			}
			workScheduler.delete(uuids)
			onActionDone.call(ReversibleAction(R.string.downloads_removed, null))
		}
	}

	fun removeCompleted() {
		launchJob(Dispatchers.Default) {
			workScheduler.removeCompleted()
			onActionDone.call(ReversibleAction(R.string.downloads_removed, null))
		}
	}

	fun snapshot(ids: Set<Long>): Collection<DownloadItemModel> {
		return works.value?.filterTo(ArrayList(ids.size)) { x -> x.id.mostSignificantBits in ids }.orEmpty()
	}

	fun allIds(): Set<Long> = works.value?.mapToSet {
		it.id.mostSignificantBits
	} ?: emptySet()

	private suspend fun List<WorkInfo>.toDownloadsList(): List<DownloadItemModel> {
		if (isEmpty()) {
			return emptyList()
		}
		val list = mapNotNullTo(ArrayList(size)) { it.toUiModel() }
		list.sortByDescending { it.timestamp }
		return list
	}

	private fun List<DownloadItemModel>.toUiList(): List<ListModel> {
		if (isEmpty()) {
			return emptyStateList()
		}
		val queued = LinkedList<ListModel>()
		val running = LinkedList<ListModel>()
		val destination = ArrayDeque<ListModel>((size * 1.4).toInt())
		var prevDate: DateTimeAgo? = null
		for (item in this) {
			when (item.workState) {
				WorkInfo.State.RUNNING -> running += item
				WorkInfo.State.BLOCKED,
				WorkInfo.State.ENQUEUED -> queued += item

				else -> {
					val date = timeAgo(item.timestamp)
					if (prevDate != date) {
						destination += ListHeader(date)
					}
					prevDate = date
					destination += item
				}
			}
		}
		if (running.isNotEmpty()) {
			running.addFirst(ListHeader(R.string.in_progress))
		}
		destination.addAll(0, running)
		if (queued.isNotEmpty()) {
			queued.addFirst(ListHeader(R.string.queued))
		}
		destination.addAll(0, queued)
		return destination
	}

	private suspend fun WorkInfo.toUiModel(): DownloadItemModel? {
		val workData = if (outputData == Data.EMPTY) progress else outputData
		val mangaId = DownloadState.getMangaId(workData)
		if (mangaId == 0L) return null
		val manga = getManga(mangaId) ?: return null
		return DownloadItemModel(
			id = id,
			workState = state,
			manga = manga,
			error = DownloadState.getError(workData),
			isIndeterminate = DownloadState.isIndeterminate(workData),
			isPaused = DownloadState.isPaused(workData),
			max = DownloadState.getMax(workData),
			progress = DownloadState.getProgress(workData),
			eta = DownloadState.getEta(workData),
			timestamp = DownloadState.getTimestamp(workData),
			totalChapters = DownloadState.getDownloadedChapters(workData).size,
		)
	}

	private fun timeAgo(date: Date): DateTimeAgo {
		val diff = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
		val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
		val diffDays = -date.daysDiff(System.currentTimeMillis())
		return when {
			diffMinutes < 3 -> DateTimeAgo.JustNow
			diffDays < 1 -> DateTimeAgo.Today
			diffDays == 1 -> DateTimeAgo.Yesterday
			diffDays < 6 -> DateTimeAgo.DaysAgo(diffDays)
			else -> DateTimeAgo.Absolute(date)
		}
	}

	private fun emptyStateList() = listOf(
		EmptyState(
			icon = R.drawable.ic_empty_common,
			textPrimary = R.string.text_downloads_list_holder,
			textSecondary = 0,
			actionStringRes = 0,
		),
	)

	private suspend fun getManga(mangaId: Long): Manga? {
		mangaCache[mangaId]?.let {
			return it
		}
		return cacheMutex.withLock {
			mangaCache.getOrElse(mangaId) {
				mangaDataRepository.findMangaById(mangaId)?.also { mangaCache[mangaId] = it } ?: return null
			}
		}
	}
}
