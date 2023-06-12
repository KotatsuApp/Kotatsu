package org.koitharu.kotatsu.download.domain

import androidx.work.Data
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.Date

data class DownloadState(
	val manga: Manga,
	val isIndeterminate: Boolean,
	val isPaused: Boolean = false,
	val isStopped: Boolean = false,
	val error: String? = null,
	val totalChapters: Int = 0,
	val currentChapter: Int = 0,
	val totalPages: Int = 0,
	val currentPage: Int = 0,
	val eta: Long = -1L,
	val localManga: LocalManga? = null,
	val downloadedChapters: LongArray = LongArray(0),
	val timestamp: Long = System.currentTimeMillis(),
) {

	val max: Int = totalChapters * totalPages

	val progress: Int = totalPages * currentChapter + currentPage + 1

	val percent: Float = if (max > 0) progress.toFloat() / max else PROGRESS_NONE

	val isFinalState: Boolean
		get() = localManga != null || (error != null && !isPaused)

	val isParticularProgress: Boolean
		get() = localManga == null && error == null && !isPaused && !isStopped && max > 0 && !isIndeterminate

	fun toWorkData() = Data.Builder()
		.putLong(DATA_MANGA_ID, manga.id)
		.putInt(DATA_MAX, max)
		.putInt(DATA_PROGRESS, progress)
		.putLong(DATA_ETA, eta)
		.putLong(DATA_TIMESTAMP, timestamp)
		.putString(DATA_ERROR, error)
		.putLongArray(DATA_CHAPTERS, downloadedChapters)
		.putBoolean(DATA_INDETERMINATE, isIndeterminate)
		.putBoolean(DATA_PAUSED, isPaused)
		.build()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as DownloadState

		if (manga != other.manga) return false
		if (isIndeterminate != other.isIndeterminate) return false
		if (isPaused != other.isPaused) return false
		if (isStopped != other.isStopped) return false
		if (error != other.error) return false
		if (totalChapters != other.totalChapters) return false
		if (currentChapter != other.currentChapter) return false
		if (totalPages != other.totalPages) return false
		if (currentPage != other.currentPage) return false
		if (eta != other.eta) return false
		if (localManga != other.localManga) return false
		if (!downloadedChapters.contentEquals(other.downloadedChapters)) return false
		if (timestamp != other.timestamp) return false
		if (max != other.max) return false
		if (progress != other.progress) return false
		return percent == other.percent
	}

	override fun hashCode(): Int {
		var result = manga.hashCode()
		result = 31 * result + isIndeterminate.hashCode()
		result = 31 * result + isPaused.hashCode()
		result = 31 * result + isStopped.hashCode()
		result = 31 * result + (error?.hashCode() ?: 0)
		result = 31 * result + totalChapters
		result = 31 * result + currentChapter
		result = 31 * result + totalPages
		result = 31 * result + currentPage
		result = 31 * result + eta.hashCode()
		result = 31 * result + (localManga?.hashCode() ?: 0)
		result = 31 * result + downloadedChapters.contentHashCode()
		result = 31 * result + timestamp.hashCode()
		result = 31 * result + max
		result = 31 * result + progress
		result = 31 * result + percent.hashCode()
		return result
	}

	companion object {

		private const val DATA_MANGA_ID = "manga_id"
		private const val DATA_MAX = "max"
		private const val DATA_PROGRESS = "progress"
		private const val DATA_CHAPTERS = "chapter"
		private const val DATA_ETA = "eta"
		private const val DATA_TIMESTAMP = "timestamp"
		private const val DATA_ERROR = "error"
		private const val DATA_INDETERMINATE = "indeterminate"
		private const val DATA_PAUSED = "paused"

		fun getMangaId(data: Data): Long = data.getLong(DATA_MANGA_ID, 0L)

		fun isIndeterminate(data: Data): Boolean = data.getBoolean(DATA_INDETERMINATE, false)

		fun isPaused(data: Data): Boolean = data.getBoolean(DATA_PAUSED, false)

		fun getMax(data: Data): Int = data.getInt(DATA_MAX, 0)

		fun getError(data: Data): String? = data.getString(DATA_ERROR)

		fun getProgress(data: Data): Int = data.getInt(DATA_PROGRESS, 0)

		fun getEta(data: Data): Long = data.getLong(DATA_ETA, -1L)

		fun getTimestamp(data: Data): Date = Date(data.getLong(DATA_TIMESTAMP, 0L))

		fun getDownloadedChapters(data: Data): LongArray = data.getLongArray(DATA_CHAPTERS) ?: LongArray(0)
	}
}
