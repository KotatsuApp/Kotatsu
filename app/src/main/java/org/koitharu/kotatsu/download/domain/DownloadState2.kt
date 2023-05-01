package org.koitharu.kotatsu.download.domain

import androidx.work.Data
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.UUID

data class DownloadState2(
	val id: UUID,
	val manga: Manga,
	val state: State,
	val error: Throwable? = null,
	val totalChapters: Int = 0,
	val currentChapter: Int = 0,
	val totalPages: Int = 0,
	val currentPage: Int = 0,
	val timeLeft: Long = -1L,
	val localManga: LocalManga? = null,
) {

	val isTerminal: Boolean
		get() = state == State.FAILED || state == State.CANCELLED || state == State.DONE

	val max: Int = totalChapters * totalPages

	val progress: Int = totalPages * currentChapter + currentPage + 1

	val percent: Float = if (max > 0) progress.toFloat() / max else PROGRESS_NONE

	fun toWorkData() = Data.Builder()
		.putString(DATA_UUID, id.toString())
		.putLong(DATA_MANGA_ID, manga.id)
		.putString(DATA_STATE, state.name)
		.build()

	enum class State {

		PREPARING, PROGRESS, PAUSED, FAILED, CANCELLED, DONE
	}

	companion object {

		private const val DATA_UUID = "uuid"
		private const val DATA_MANGA_ID = "manga_id"
		private const val DATA_STATE = "state"
	}
}
