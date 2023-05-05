package org.koitharu.kotatsu.download.domain

import androidx.work.Data
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.Date

data class DownloadState2(
	val manga: Manga,
	val isIndeterminate: Boolean,
	val isPaused: Boolean = false,
	val error: Throwable? = null,
	val totalChapters: Int = 0,
	val currentChapter: Int = 0,
	val totalPages: Int = 0,
	val currentPage: Int = 0,
	val eta: Long = -1L,
	val localManga: LocalManga? = null,
	val timestamp: Long = System.currentTimeMillis(),
) {

	val max: Int = totalChapters * totalPages

	val progress: Int = totalPages * currentChapter + currentPage + 1

	val percent: Float = if (max > 0) progress.toFloat() / max else PROGRESS_NONE

	val isFinalState: Boolean
		get() = localManga != null || (error != null && !isPaused)

	fun toWorkData() = Data.Builder()
		.putLong(DATA_MANGA_ID, manga.id)
		.putInt(DATA_MAX, max)
		.putInt(DATA_PROGRESS, progress)
		.putLong(DATA_ETA, eta)
		.putLong(DATA_TIMESTAMP, timestamp)
		.putString(DATA_ERROR, error?.toString())
		.build()

	companion object {

		private const val DATA_MANGA_ID = "manga_id"
		private const val DATA_MAX = "max"
		private const val DATA_PROGRESS = "progress"
		private const val DATA_ETA = "eta"
		private const val DATA_TIMESTAMP = "timestamp"
		private const val DATA_ERROR = "error"

		fun getMangaId(data: Data): Long = data.getLong(DATA_MANGA_ID, 0L)

		fun getMax(data: Data) = data.getInt(DATA_MAX, 0)

		fun getProgress(data: Data) = data.getInt(DATA_PROGRESS, 0)

		fun getEta(data: Data) = data.getLong(DATA_ETA, -1L)

		fun getTimestamp(data: Data) = Date(data.getLong(DATA_TIMESTAMP, 0L))
	}
}
