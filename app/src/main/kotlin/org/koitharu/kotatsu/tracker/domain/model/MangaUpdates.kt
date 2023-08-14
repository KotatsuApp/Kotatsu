package org.koitharu.kotatsu.tracker.domain.model

import org.koitharu.kotatsu.core.exceptions.TooManyRequestExceptions
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter

sealed interface MangaUpdates {

	val manga: Manga

	class Success(
		override val manga: Manga,
		val newChapters: List<MangaChapter>,
		val isValid: Boolean,
	) : MangaUpdates {

		fun isNotEmpty() = newChapters.isNotEmpty()
	}

	class Failure(
		override val manga: Manga,
		val error: Throwable?,
	) : MangaUpdates {

		fun shouldRetry() = error is TooManyRequestExceptions
	}
}
