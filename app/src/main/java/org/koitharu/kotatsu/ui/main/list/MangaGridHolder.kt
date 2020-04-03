package org.koitharu.kotatsu.ui.main.list

import android.view.ViewGroup
import coil.api.clear
import coil.api.load
import kotlinx.android.synthetic.main.item_manga_grid.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class MangaGridHolder(parent: ViewGroup) : BaseViewHolder<Manga, MangaHistory?>(parent, R.layout.item_manga_grid) {

	override fun onBind(data: Manga, extra: MangaHistory?) {
		imageView_cover.clear()
		textView_title.text = data.title
		imageView_cover.load(data.coverUrl) {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_placeholder)
		}
	}

	override fun onRecycled() {
		imageView_cover.clear()
	}
}