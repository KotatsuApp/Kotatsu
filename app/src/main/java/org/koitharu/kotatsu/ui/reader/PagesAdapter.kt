package org.koitharu.kotatsu.ui.reader

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class PagesAdapter(private val loader: PageLoader) : BaseRecyclerAdapter<MangaPage>() {

	override fun onCreateViewHolder(parent: ViewGroup) = PageHolder(parent, loader)

	override fun onGetItemId(item: MangaPage) = item.id
}