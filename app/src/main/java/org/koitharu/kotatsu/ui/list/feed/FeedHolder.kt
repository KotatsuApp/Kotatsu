package org.koitharu.kotatsu.ui.list.feed

import android.text.format.DateUtils
import android.view.ViewGroup
import coil.ImageLoader
import coil.request.Disposable
import kotlinx.android.synthetic.main.item_tracklog.*
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.formatRelative
import org.koitharu.kotatsu.utils.ext.newImageRequest

class FeedHolder(parent: ViewGroup) :
	BaseViewHolder<TrackingLogItem, Unit>(parent, R.layout.item_tracklog) {

	private val coil by inject<ImageLoader>()
	private var imageRequest: Disposable? = null

	override fun onBind(data: TrackingLogItem, extra: Unit) {
		imageRequest?.dispose()
		imageRequest = imageView_cover.newImageRequest(data.manga.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.enqueueWith(coil)
		textView_title.text = data.manga.title
		textView_subtitle.text = buildString {
			append(data.createdAt.formatRelative(DateUtils.DAY_IN_MILLIS))
			append(" ")
			append(
				context.resources.getQuantityString(
					R.plurals.new_chapters,
					data.chapters.size,
					data.chapters.size
				)
			)
		}
		textView_chapters.text = data.chapters.joinToString("\n")
	}

	override fun onRecycled() {
		imageRequest?.dispose()
		imageView_cover.setImageDrawable(null)
	}
}