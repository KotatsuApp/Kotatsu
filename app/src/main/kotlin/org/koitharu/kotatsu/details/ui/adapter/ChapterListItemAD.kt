package org.koitharu.kotatsu.details.ui.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemChapterBinding
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import com.google.android.material.R as materialR

fun chapterListItemAD(
	clickListener: OnListItemClickListener<ChapterListItem>,
) = adapterDelegateViewBinding<ChapterListItem, ChapterListItem, ItemChapterBinding>(
	{ inflater, parent -> ItemChapterBinding.inflate(inflater, parent, false) },
) {

	val eventListener = AdapterDelegateClickListenerAdapter(this, clickListener)
	itemView.setOnClickListener(eventListener)
	itemView.setOnLongClickListener(eventListener)

	bind { payloads ->
		if (payloads.isEmpty()) {
			binding.textViewTitle.text = item.chapter.name
			binding.textViewNumber.text = item.chapter.number.toString()
			binding.textViewDescription.textAndVisible = item.description()
		}
		when {
			item.isCurrent -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_primary)
				binding.textViewNumber.setTextColor(context.getThemeColor(materialR.attr.colorOnPrimary))
			}

			item.isUnread -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_default)
				binding.textViewNumber.setTextColor(context.getThemeColor(materialR.attr.colorOnTertiary))
			}

			else -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_outline)
				binding.textViewNumber.setTextColor(context.getThemeColor(android.R.attr.textColorTertiary))
			}
		}
		binding.imageViewBookmarked.isVisible = item.isBookmarked
		binding.imageViewDownloaded.isVisible = item.isDownloaded
		// binding.imageViewNew.isVisible = item.isNew
		binding.textViewTitle.drawableStart = if (item.isNew) {
			ContextCompat.getDrawable(context, R.drawable.ic_new)
		} else {
			null
		}
	}
}
