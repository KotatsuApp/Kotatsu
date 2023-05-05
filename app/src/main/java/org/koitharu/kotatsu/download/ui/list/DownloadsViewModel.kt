package org.koitharu.kotatsu.download.ui.list

import androidx.collection.LongSparseArray
import androidx.collection.getOrElse
import androidx.collection.set
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.WorkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapLatest
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

	val items = workScheduler.observeWorks()
		.mapLatest { list ->
			list.mapList()
		}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	private suspend fun List<WorkInfo>.mapList(): List<ListModel> {
		val destination = ArrayList<ListModel>((size * 1.4).toInt())
		var prevDate: DateTimeAgo? = null
		for (item in this) {
			val model = item.toUiModel() ?: continue
			val date = timeAgo(model.createdAt)
			if (prevDate != date) {
				destination += date
			}
			prevDate = date
			destination += model
		}
		if (destination.isEmpty()) {
			destination.add(
				EmptyState(
					icon = R.drawable.ic_empty_common,
					textPrimary = R.string.text_downloads_holder,
					textSecondary = 0,
					actionStringRes = 0,
				),
			)
		}
		return destination
	}

	private suspend fun WorkInfo.toUiModel(): DownloadItemModel? {
		val workData = if (progress != Data.EMPTY) progress else outputData
		val mangaId = DownloadState2.getMangaId(workData)
		if (mangaId == 0L) return null
		val manga = mangaCache.getOrElse(mangaId) {
			mangaDataRepository.findMangaById(mangaId)?.also { mangaCache[mangaId] = it } ?: return null
		}
		return DownloadItemModel(
			id = id,
			workState = state,
			manga = manga,
			error = null,
			max = DownloadState2.getMax(workData),
			progress = DownloadState2.getProgress(workData),
			eta = DownloadState2.getEta(workData),
			createdAt = DownloadState2.getTimestamp(workData),
		)
	}

	fun cancel(id: UUID) {
		launchJob(Dispatchers.Default) {
			workScheduler.cancel(id)
		}
	}

	fun restart(id: UUID) {
		// TODO
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
}
