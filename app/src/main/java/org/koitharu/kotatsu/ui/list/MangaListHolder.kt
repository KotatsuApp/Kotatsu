package org.koitharu.kotatsu.ui.list

import android.view.ViewGroup
import coil.clear
import coil.load
import kotlinx.android.synthetic.main.item_manga_list.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.textAndVisible

class MangaListHolder(parent: ViewGroup) :
	BaseViewHolder<Manga, MangaHistory?>(parent, R.layout.item_manga_list) {

	override fun onBind(data: Manga, extra: MangaHistory?) {
		imageView_cover.clear()
		textView_title.text = data.title
		textView_subtitle.textAndVisible = data.tags.joinToString(", ") { it.title }
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