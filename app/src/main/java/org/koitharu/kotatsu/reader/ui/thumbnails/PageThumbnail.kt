package org.koitharu.kotatsu.reader.ui.thumbnails

import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.parser.MangaRepository

data class PageThumbnail(
	val number: Int,
	val isCurrent: Boolean,
	val repository: MangaRepository,
	val page: MangaPage
)