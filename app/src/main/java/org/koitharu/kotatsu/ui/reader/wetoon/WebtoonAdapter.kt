package org.koitharu.kotatsu.ui.reader.wetoon

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.GroupedList

class WebtoonAdapter(
	pages: GroupedList<Long, MangaPage>,
	private val loader: PageLoader
) : BaseReaderAdapter(pages) {

	override fun onCreateViewHolder(parent: ViewGroup) = WebtoonHolder(parent, loader)
}