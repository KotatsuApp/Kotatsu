package org.koitharu.kotatsu.details.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import kotlinx.android.synthetic.main.item_chapter.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.history.domain.ChapterExtra
import org.koitharu.kotatsu.utils.ext.getThemeColor

fun chapterListItemAD(
	clickListener: OnListItemClickListener<MangaChapter>
) = adapterDelegateLayoutContainer<ChapterListItem, ChapterListItem>(R.layout.item_chapter) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item.chapter, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item.chapter, it)
	}

	bind { payload ->
		textView_title.text = item.chapter.name
		textView_number.text = item.chapter.number.toString()
		when (item.extra) {
			ChapterExtra.UNREAD -> {
				textView_number.setBackgroundResource(R.drawable.bg_badge_default)
				textView_number.setTextColor(context.getThemeColor(android.R.attr.textColorSecondaryInverse))
			}
			ChapterExtra.READ -> {
				textView_number.setBackgroundResource(R.drawable.bg_badge_outline)
				textView_number.setTextColor(context.getThemeColor(android.R.attr.textColorTertiary))
			}
			ChapterExtra.CURRENT -> {
				textView_number.setBackgroundResource(R.drawable.bg_badge_outline_accent)
				textView_number.setTextColor(context.getThemeColor(androidx.appcompat.R.attr.colorAccent))
			}
			ChapterExtra.NEW -> {
				textView_number.setBackgroundResource(R.drawable.bg_badge_accent)
				textView_number.setTextColor(context.getThemeColor(android.R.attr.textColorPrimaryInverse))
			}
		}
	}
}