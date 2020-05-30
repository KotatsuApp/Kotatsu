package org.koitharu.kotatsu.ui.list.feed

import android.text.format.DateUtils
import android.view.ViewGroup
import coil.api.clear
import coil.api.load
import kotlinx.android.synthetic.main.item_tracklog.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.formatRelative

class FeedHolder(parent: ViewGroup) :
	BaseViewHolder<TrackingLogItem, Unit>(parent, R.layout.item_tracklog) {

	override fun onBind(data: TrackingLogItem, extra: Unit) {
		imageView_cover.load(data.manga.coverUrl) {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_placeholder)
		}
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
		super.onRecycled()
		imageView_cover.clear()
	}
}