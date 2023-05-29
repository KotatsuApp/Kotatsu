package org.koitharu.kotatsu.download.ui.list

import android.text.format.DateUtils
import androidx.work.WorkInfo
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.Date
import java.util.UUID

class DownloadItemModel(
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

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as DownloadItemModel

		if (id != other.id) return false
		if (workState != other.workState) return false
		if (isIndeterminate != other.isIndeterminate) return false
		if (isPaused != other.isPaused) return false
		if (manga != other.manga) return false
		if (error != other.error) return false
		if (max != other.max) return false
		if (totalChapters != other.totalChapters) return false
		if (progress != other.progress) return false
		if (eta != other.eta) return false
		return timestamp == other.timestamp
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + workState.hashCode()
		result = 31 * result + isIndeterminate.hashCode()
		result = 31 * result + isPaused.hashCode()
		result = 31 * result + manga.hashCode()
		result = 31 * result + (error?.hashCode() ?: 0)
		result = 31 * result + max
		result = 31 * result + totalChapters
		result = 31 * result + progress
		result = 31 * result + eta.hashCode()
		result = 31 * result + timestamp.hashCode()
		return result
	}
}
