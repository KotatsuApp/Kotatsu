package org.koitharu.kotatsu.details.ui

import android.graphics.Color
import android.view.ViewGroup
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_chapter.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.history.domain.ChapterExtra
import org.koitharu.kotatsu.utils.ext.getThemeColor

class ChapterHolder(parent: ViewGroup) :
	BaseViewHolder<MangaChapter, ChapterExtra>(parent, R.layout.item_chapter) {

	override fun onBind(data: MangaChapter, extra: ChapterExtra) {
		textView_title.text = data.name
		textView_number.text = data.number.toString()
		imageView_check.isVisible = extra == ChapterExtra.CHECKED
		when (extra) {
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
			ChapterExtra.CHECKED -> {
				textView_number.background = null
				textView_number.setTextColor(Color.TRANSPARENT)
			}
		}
	}
}