package org.koitharu.kotatsu.ui.reader.standard

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.reader.PageLoader

class PagesAdapter(private val loader: PageLoader) : BaseRecyclerAdapter<MangaPage, Unit>() {

	override fun onCreateViewHolder(parent: ViewGroup) =
		PageHolder(parent, loader)

	override fun onGetItemId(item: MangaPage) = item.id

	override fun getExtra(item: MangaPage, position: Int) = Unit
}