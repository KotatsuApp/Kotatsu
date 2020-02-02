package org.koitharu.kotatsu.ui.main.list

import android.view.ViewGroup
import coil.api.load
import coil.request.RequestDisposable
import kotlinx.android.synthetic.main.item_manga_grid.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class MangaGridHolder(parent: ViewGroup) : BaseViewHolder<Manga, MangaHistory?>(parent, R.layout.item_manga_grid) {

	private var coverRequest: RequestDisposable? = null

	override fun onBind(data: Manga, extra: MangaHistory?) {
		coverRequest?.dispose()
		textView_title.text = data.title
		coverRequest = imageView_cover.load(data.coverUrl) {
			crossfade(true)
		}
	}
}