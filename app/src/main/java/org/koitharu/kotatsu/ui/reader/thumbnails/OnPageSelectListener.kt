package org.koitharu.kotatsu.ui.reader.thumbnails

import org.koitharu.kotatsu.core.model.MangaPage

fun interface OnPageSelectListener {

	fun onPageSelected(page: MangaPage)
}