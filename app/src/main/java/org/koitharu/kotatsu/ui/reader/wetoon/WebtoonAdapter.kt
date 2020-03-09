package org.koitharu.kotatsu.ui.reader.wetoon

import android.view.Gravity
import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.GroupedList
import org.koitharu.kotatsu.ui.reader.PageLoader

class WebtoonAdapter(
	pages: GroupedList<Long, MangaPage>,
	private val loader: PageLoader
) : BaseReaderAdapter<Int>(pages) {

	var pageGravity: Int = Gravity.TOP

	override fun onCreateViewHolder(parent: ViewGroup) = WebtoonHolder(parent, loader)

	override fun getExtra(item: MangaPage, position: Int) = pageGravity
}