package org.koitharu.kotatsu.download.ui.list

import android.content.Context
import android.graphics.Color
import android.text.format.DateUtils
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.work.WorkInfo
import coil3.memory.MemoryCache
import kotlinx.coroutines.flow.StateFlow
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.download.ui.list.chapters.DownloadChapter
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import java.time.Instant
import java.util.UUID
import com.google.android.material.R as materialR

data class DownloadItemModel(
	val id: UUID,
	val workState: WorkInfo.State,
	val isIndeterminate: Boolean,
	val isPaused: Boolean,
	val manga: Manga?,
	val error: String?,
	val max: Int,
	val progress: Int,
	val eta: Long,
	val isStuck: Boolean,
	val timestamp: Instant,
	val chaptersDownloaded: Int,
	val isExpanded: Boolean,
	val chapters: StateFlow<List<DownloadChapter>?>,
) : ListModel, Comparable<DownloadItemModel> {

	val coverCacheKey = MemoryCache.Key(manga?.coverUrl.orEmpty(), mapOf("dl" to "1"))

	val percent: Float
		get() = if (max > 0) progress / max.toFloat() else 0f

	val hasEta: Boolean
		get() = workState == WorkInfo.State.RUNNING && !isPaused && eta > 0L

	val canPause: Boolean
		get() = workState == WorkInfo.State.RUNNING && !isPaused && error == null

	val canResume: Boolean
		get() = workState == WorkInfo.State.RUNNING && isPaused

	fun getEtaString(): CharSequence? = if (hasEta) {
		DateUtils.getRelativeTimeSpanString(
			eta,
			System.currentTimeMillis(),
			DateUtils.SECOND_IN_MILLIS,
		)
	} else {
		null
	}

	fun getErrorMessage(context: Context): CharSequence? = if (error != null) {
		buildSpannedString {
			bold {
				color(context.getThemeColor(materialR.attr.colorError, Color.RED)) {
					append(error)
				}
			}
		}
	} else {
		null
	}

	override fun compareTo(other: DownloadItemModel): Int {
		return timestamp.compareTo(other.timestamp)
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is DownloadItemModel && other.id == id
	}

	override fun getChangePayload(previousState: ListModel): Any? = when {
		previousState !is DownloadItemModel -> super.getChangePayload(previousState)
		workState != previousState.workState -> null
		isExpanded != previousState.isExpanded -> ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		else -> ListModelDiffCallback.PAYLOAD_ANYTHING_CHANGED
	}
}
