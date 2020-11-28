package org.koitharu.kotatsu.reader.ui.thumbnails

import org.koitharu.kotatsu.core.model.MangaPage

fun interface OnPageSelectListener {

	fun onPageSelected(page: MangaPage)
}