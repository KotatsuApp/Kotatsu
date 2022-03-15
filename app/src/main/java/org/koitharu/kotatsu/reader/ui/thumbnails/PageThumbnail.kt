package org.koitharu.kotatsu.reader.ui.thumbnails

import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.parsers.model.MangaPage

data class PageThumbnail(
	val number: Int,
	val isCurrent: Boolean,
	val repository: MangaRepository,
	val page: MangaPage
)