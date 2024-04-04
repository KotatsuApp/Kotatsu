package org.koitharu.kotatsu.reader.ui.thumbnails

import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

@Deprecated("")
fun interface OnPageSelectListener {

	fun onPageSelected(page: ReaderPage): Boolean
}
