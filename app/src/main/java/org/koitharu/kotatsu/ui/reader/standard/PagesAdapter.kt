package org.koitharu.kotatsu.ui.reader.standard

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.GroupedList
import org.koitharu.kotatsu.ui.reader.PageLoader

class PagesAdapter(
	pages: GroupedList<Long, MangaPage>,
	private val loader: PageLoader
) : BaseReaderAdapter<Unit>(pages) {

	override fun onCreateViewHolder(parent: ViewGroup) = PageHolder(parent, loader)

	override fun getExtra(item: MangaPage, position: Int) = Unit
}