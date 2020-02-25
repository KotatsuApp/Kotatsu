package org.koitharu.kotatsu.ui.reader.wetoon

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.reader.PageLoader

class WebtoonAdapter(private val loader: PageLoader) : BaseRecyclerAdapter<MangaPage, Unit>() {

	override fun onCreateViewHolder(parent: ViewGroup) =
		WebtoonHolder(parent, loader)

	override fun onGetItemId(item: MangaPage) = item.id

	override fun getExtra(item: MangaPage, position: Int) = Unit
}