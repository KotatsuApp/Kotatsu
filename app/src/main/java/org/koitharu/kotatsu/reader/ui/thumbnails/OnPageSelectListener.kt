package org.koitharu.kotatsu.reader.ui.thumbnails

import org.koitharu.kotatsu.parsers.model.MangaPage

fun interface OnPageSelectListener {

	fun onPageSelected(page: MangaPage)
}