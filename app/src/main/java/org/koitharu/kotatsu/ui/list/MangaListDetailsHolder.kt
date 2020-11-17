package org.koitharu.kotatsu.ui.list

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import coil.request.Disposable
import kotlinx.android.synthetic.main.item_manga_list_details.*
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.textAndVisible
import kotlin.math.roundToInt

class MangaListDetailsHolder(
	parent: ViewGroup
) : BaseViewHolder<Manga, MangaHistory?>(parent, R.layout.item_manga_list_details) {

	private val coil by inject<ImageLoader>()
	private var imageRequest: Disposable? = null

	@SuppressLint("SetTextI18n")
	override fun onBind(data: Manga, extra: MangaHistory?) {
		imageRequest?.dispose()
		textView_title.text = data.title
		textView_subtitle.textAndVisible = data.altTitle
		imageRequest = imageView_cover.newImageRequest(data.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.enqueueWith(coil)
		if (data.rating == Manga.NO_RATING) {
			textView_rating.isVisible = false
		} else {
			textView_rating.text = "${(data.rating * 10).roundToInt()}/10"
			textView_rating.isVisible = true
		}
		textView_tags.text = data.tags.joinToString(", ") {
			it.title
		}
	}

	override fun onRecycled() {
		imageRequest?.dispose()
		imageView_cover.setImageDrawable(null)
	}
}