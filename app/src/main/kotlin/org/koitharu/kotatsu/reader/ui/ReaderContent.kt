package org.koitharu.kotatsu.reader.ui

import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

data class ReaderContent(
	val pages: List<ReaderPage>,
	val state: ReaderState?
)