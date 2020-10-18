package org.koitharu.kotatsu.ui.list

import android.view.ViewGroup
import coil.ImageLoader
import coil.request.Disposable
import kotlinx.android.synthetic.main.item_manga_grid.*
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest

class MangaGridHolder(parent: ViewGroup) :
	BaseViewHolder<Manga, MangaHistory?>(parent, R.layout.item_manga_grid) {

	private val coil by inject<ImageLoader>()
	private var imageRequest: Disposable? = null

	override fun onBind(data: Manga, extra: MangaHistory?) {
		textView_title.text = data.title
		imageRequest?.dispose()
		imageRequest = imageView_cover.newImageRequest(data.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.enqueueWith(coil)
	}

	override fun onRecycled() {
		imageRequest?.dispose()
		imageView_cover.setImageDrawable(null)
	}
}