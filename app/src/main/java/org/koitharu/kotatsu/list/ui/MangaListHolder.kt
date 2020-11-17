package org.koitharu.kotatsu.list.ui

import android.view.ViewGroup
import coil.ImageLoader
import coil.request.Disposable
import kotlinx.android.synthetic.main.item_manga_list.*
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.textAndVisible

class MangaListHolder(
	parent: ViewGroup
) : BaseViewHolder<Manga, MangaHistory?>(parent, R.layout.item_manga_list) {

	private val coil by inject<ImageLoader>()
	private var imageRequest: Disposable? = null

	override fun onBind(data: Manga, extra: MangaHistory?) {
		imageRequest?.dispose()
		textView_title.text = data.title
		textView_subtitle.textAndVisible = data.tags.joinToString(", ") { it.title }
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