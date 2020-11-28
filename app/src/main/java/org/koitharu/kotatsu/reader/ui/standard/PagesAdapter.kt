package org.koitharu.kotatsu.reader.ui.standard

import android.view.ViewGroup
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.base.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.base.ReaderPage

class PagesAdapter(
	pages: List<ReaderPage>,
	private val loader: PageLoader
) : BaseReaderAdapter(pages) {

	override fun onCreateViewHolder(parent: ViewGroup) = PageHolder(parent, loader)
}