package org.koitharu.kotatsu.reader.ui.thumbnails

import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

class PageThumbnail(
	val isCurrent: Boolean,
	val repository: MangaRepository,
	val page: ReaderPage,
) : ListModel {

	val number
		get() = page.index + 1

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as PageThumbnail

		if (isCurrent != other.isCurrent) return false
		if (repository != other.repository) return false
		return page == other.page
	}

	override fun hashCode(): Int {
		var result = isCurrent.hashCode()
		result = 31 * result + repository.hashCode()
		result = 31 * result + page.hashCode()
		return result
	}

}
