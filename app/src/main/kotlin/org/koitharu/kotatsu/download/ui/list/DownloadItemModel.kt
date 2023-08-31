package org.koitharu.kotatsu.download.ui.list

import android.text.format.DateUtils
import androidx.work.WorkInfo
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.Date
import java.util.UUID

data class DownloadItemModel(
	val id: UUID,
	val workState: WorkInfo.State,
	val isIndeterminate: Boolean,
	val isPaused: Boolean,
	val manga: Manga,
	val error: String?,
	val max: Int,
	val totalChapters: Int,
	val progress: Int,
	val eta: Long,
	val timestamp: Date,
) : ListModel, Comparable<DownloadItemModel> {

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

	override fun compareTo(other: DownloadItemModel): Int {
		return timestamp.compareTo(other.timestamp)
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is DownloadItemModel && other.id == id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return when (previousState) {
			is DownloadItemModel -> {
				if (workState == previousState.workState) {
					Unit
				} else {
					null
				}
			}

			else -> super.getChangePayload(previousState)
		}
	}
}
