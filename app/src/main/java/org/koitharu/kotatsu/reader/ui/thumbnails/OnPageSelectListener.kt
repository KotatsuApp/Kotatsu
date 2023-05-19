package org.koitharu.kotatsu.reader.ui.thumbnails

import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

fun interface OnPageSelectListener {

	fun onPageSelected(page: ReaderPage)
}
