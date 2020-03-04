package org.koitharu.kotatsu.ui.reader.wetoon

import android.view.Gravity
import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.ui.reader.PageLoader

class WebtoonAdapter(private val loader: PageLoader) : BaseRecyclerAdapter<MangaPage, Int>() {

	private var lastBound = -1

	override fun onCreateViewHolder(parent: ViewGroup) =
		WebtoonHolder(parent, loader)

	override fun onGetItemId(item: MangaPage) = item.id

	override fun onBindViewHolder(holder: BaseViewHolder<MangaPage, Int>, position: Int) {
		super.onBindViewHolder(holder, position)
		lastBound = position
	}

	override fun getExtra(item: MangaPage, position: Int) = if (position >= lastBound) {
		Gravity.TOP
	} else {
		Gravity.BOTTOM
	}
}