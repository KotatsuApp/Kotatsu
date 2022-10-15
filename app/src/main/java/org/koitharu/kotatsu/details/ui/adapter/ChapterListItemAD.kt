package org.koitharu.kotatsu.details.ui.adapter

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemChapterBinding
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_CURRENT
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_DOWNLOADED
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_MISSING
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_NEW
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_UNREAD
import org.koitharu.kotatsu.utils.ext.getThemeColor
import org.koitharu.kotatsu.utils.ext.textAndVisible

fun chapterListItemAD(
	clickListener: OnListItemClickListener<ChapterListItem>,
) = adapterDelegateViewBinding<ChapterListItem, ChapterListItem, ItemChapterBinding>(
	{ inflater, parent -> ItemChapterBinding.inflate(inflater, parent, false) }
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
		when (item.status) {
			FLAG_UNREAD -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_default)
				binding.textViewNumber.setTextColor(context.getThemeColor(com.google.android.material.R.attr.colorOnTertiary))
			}
			FLAG_CURRENT -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_accent)
				binding.textViewNumber.setTextColor(context.getThemeColor(android.R.attr.textColorPrimaryInverse))
			}
			else -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_outline)
				binding.textViewNumber.setTextColor(context.getThemeColor(android.R.attr.textColorTertiary))
			}
		}
		val isMissing = item.hasFlag(FLAG_MISSING)
		binding.textViewTitle.alpha = if (isMissing) 0.3f else 1f
		binding.textViewDescription.alpha = if (isMissing) 0.3f else 1f
		binding.textViewNumber.alpha = if (isMissing) 0.3f else 1f

		binding.imageViewDownloaded.isVisible = item.hasFlag(FLAG_DOWNLOADED)
		binding.imageViewNew.isVisible = item.hasFlag(FLAG_NEW)
	}
}
