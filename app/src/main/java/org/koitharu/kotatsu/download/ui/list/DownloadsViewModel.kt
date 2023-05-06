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
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.download.domain.DownloadState2
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.utils.asFlowLiveData
import org.koitharu.kotatsu.utils.ext.daysDiff
import java.util.Date
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
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val items = works.map {
		it.toUiList()
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	val hasPausedWorks = works.map {
		it.any { x -> x.canResume }
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, false)

	val hasActiveWorks = works.map {
		it.any { x -> x.canPause }
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, false)

	val hasCancellableWorks = works.map {
		it.any { x -> !x.workState.isFinished }
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, false)

	fun cancel(id: UUID) {
		launchJob(Dispatchers.Default) {
			workScheduler.cancel(id)
		}
	}

	fun cancel(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			val snapshot = works.value
			for (work in snapshot) {
				if (work.id.mostSignificantBits in ids) {
					workScheduler.cancel(work.id)
				}
			}
		}
	}

	fun cancelAll() {
		launchJob(Dispatchers.Default) {
			workScheduler.cancelAll()
		}
	}

	fun pause(ids: Set<Long>) {
		val snapshot = works.value
		for (work in snapshot) {
			if (work.id.mostSignificantBits in ids) {
				workScheduler.pause(work.id)
			}
		}
	}

	fun pauseAll() {
		val snapshot = works.value
		for (work in snapshot) {
			if (work.canPause) {
				workScheduler.pause(work.id)
			}
		}
	}

	fun resumeAll() {
		val snapshot = works.value
		for (work in snapshot) {
			if (work.workState == WorkInfo.State.RUNNING && work.isPaused) {
				workScheduler.resume(work.id)
			}
		}
	}

	fun resume(ids: Set<Long>) {
		val snapshot = works.value
		for (work in snapshot) {
			if (work.id.mostSignificantBits in ids) {
				workScheduler.resume(work.id)
			}
		}
	}

	fun remove(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			val snapshot = works.value
			for (work in snapshot) {
				if (work.id.mostSignificantBits in ids) {
					workScheduler.delete(work.id)
				}
			}
		}
	}

	fun removeCompleted() {
		launchJob(Dispatchers.Default) {
			workScheduler.removeCompleted()
		}
	}

	fun snapshot(ids: Set<Long>): Collection<DownloadItemModel> {
		return works.value.filterTo(ArrayList(ids.size)) { x -> x.id.mostSignificantBits in ids }
	}

	fun allIds(): Set<Long> = works.value.mapToSet {
		it.id.mostSignificantBits
	}

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
		val destination = ArrayList<ListModel>((size * 1.4).toInt())
		var prevDate: DateTimeAgo? = null
		for (item in this) {
			val date = timeAgo(item.timestamp)
			if (prevDate != date) {
				destination += date
			}
			prevDate = date
			destination += item
		}
		return destination
	}

	private suspend fun WorkInfo.toUiModel(): DownloadItemModel? {
		val workData = if (progress != Data.EMPTY) progress else outputData
		val mangaId = DownloadState2.getMangaId(workData)
		if (mangaId == 0L) return null
		val manga = getManga(mangaId) ?: return null
		return DownloadItemModel(
			id = id,
			workState = state,
			manga = manga,
			error = DownloadState2.getError(workData),
			isIndeterminate = DownloadState2.isIndeterminate(workData),
			isPaused = DownloadState2.isPaused(workData),
			max = DownloadState2.getMax(workData),
			progress = DownloadState2.getProgress(workData),
			eta = DownloadState2.getEta(workData),
			timestamp = DownloadState2.getTimestamp(workData),
			totalChapters = DownloadState2.getTotalChapters(workData),
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
			textPrimary = R.string.text_downloads_holder,
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
