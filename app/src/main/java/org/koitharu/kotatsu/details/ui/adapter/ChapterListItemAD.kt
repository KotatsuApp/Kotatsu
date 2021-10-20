package org.koitharu.kotatsu.details.ui.adapter

import android.text.SpannableStringBuilder
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koin.core.context.GlobalContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ItemChapterBinding
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.history.domain.ChapterExtra
import org.koitharu.kotatsu.utils.ext.getThemeColor
import java.util.*

fun chapterListItemAD(
	clickListener: OnListItemClickListener<ChapterListItem>,
) = adapterDelegateViewBinding<ChapterListItem, ChapterListItem, ItemChapterBinding>(
	{ inflater, parent -> ItemChapterBinding.inflate(inflater, parent, false) }
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item, it)
	}

	bind { payload ->
		binding.textViewTitle.text = item.chapter.name
		binding.textViewNumber.text = item.chapter.number.toString()
		val settings = GlobalContext.get().get<AppSettings>()
		val descriptions = mutableListOf<CharSequence>()
		val dateFormat = settings.dateFormat()
		if (item.chapter.uploadDate > 0) {
			descriptions.add(dateFormat.format(Date(item.chapter.uploadDate)))
		}
		if (!item.chapter.scanlator.isNullOrBlank()) {
			descriptions.add(item.chapter.scanlator!!)
		}
		if (descriptions.isNotEmpty()) {
			binding.textViewDescription.text = descriptions.joinTo(SpannableStringBuilder(), " • ")
		} else {
			binding.textViewDescription.text = ""
		}
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
		binding.textViewTitle.alpha = if (item.isMissing) 0.3f else 1f
		binding.textViewDescription.alpha = if (item.isMissing) 0.3f else 1f
		binding.textViewNumber.alpha = if (item.isMissing) 0.3f else 1f
	}
}