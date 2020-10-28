package org.koitharu.kotatsu.ui.reader.wetoon

import android.view.ViewGroup
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.ReaderPage

class WebtoonAdapter(
	pages: List<ReaderPage>,
	private val loader: PageLoader
) : BaseReaderAdapter(pages) {

	override fun onCreateViewHolder(parent: ViewGroup) = WebtoonHolder(parent, loader)
}