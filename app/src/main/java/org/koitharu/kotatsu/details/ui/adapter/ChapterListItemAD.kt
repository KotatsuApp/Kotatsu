package org.koitharu.kotatsu.details.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.databinding.ItemChapterBinding
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.history.domain.ChapterExtra
import org.koitharu.kotatsu.utils.ext.getThemeColor

fun chapterListItemAD(
	clickListener: OnListItemClickListener<MangaChapter>
) = adapterDelegateViewBinding<ChapterListItem, ChapterListItem, ItemChapterBinding>(
	{ inflater, parent -> ItemChapterBinding.inflate(inflater, parent, false) }
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item.chapter, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item.chapter, it)
	}

	bind { payload ->
		binding.textViewTitle.text = item.chapter.name
		binding.textViewNumber.text = item.chapter.number.toString()
		when (item.extra) {
			ChapterExtra.UNREAD -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_default)
				binding.textViewNumber.setTextColor(context.getThemeColor(android.R.attr.textColorSecondaryInverse))
			}
			ChapterExtra.READ -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_outline)
				binding.textViewNumber.setTextColor(context.getThemeColor(android.R.attr.textColorTertiary))
			}
			ChapterExtra.CURRENT -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_outline_accent)
				binding.textViewNumber.setTextColor(context.getThemeColor(androidx.appcompat.R.attr.colorAccent))
			}
			ChapterExtra.NEW -> {
				binding.textViewNumber.setBackgroundResource(R.drawable.bg_badge_accent)
				binding.textViewNumber.setTextColor(context.getThemeColor(android.R.attr.textColorPrimaryInverse))
			}
		}
	}
}