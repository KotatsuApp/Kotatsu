package org.koitharu.kotatsu.reader.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.parsers.model.Manga

@Parcelize
data class ReaderState(
	val chapterId: Long,
	val page: Int,
	val scroll: Int
) : Parcelable {

	companion object {

		fun from(history: MangaHistory) = ReaderState(
			chapterId = history.chapterId,
			page = history.page,
			scroll = history.scroll
		)

		fun initial(manga: Manga, branch: String?) = ReaderState(
			chapterId = manga.chapters?.firstOrNull {
				it.branch == branch
			}?.id ?: error("Cannot find first chapter"),
			page = 0,
			scroll = 0
		)
	}
}