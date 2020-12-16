package org.koitharu.kotatsu.reader.ui.pager

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.model.MangaSource

@Parcelize
data class ReaderPage(
	val id: Long,
	val url: String,
	val preview: String?,
	val chapterId: Long,
	val index: Int,
	val source: MangaSource
) : Parcelable {

	fun toMangaPage() = MangaPage(
		id = id,
		url = url,
		preview = preview,
		source = source
	)

	companion object {

		fun from(page: MangaPage, index: Int, chapterId: Long) = ReaderPage(
			id = page.id,
			url = page.url,
			preview = page.preview,
			chapterId = chapterId,
			index = index,
			source = page.source
		)
	}
}