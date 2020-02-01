package org.koitharu.kotatsu.ui.details

import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_chapter.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class ChapterHolder(parent: ViewGroup) : BaseViewHolder<MangaChapter>(parent, R.layout.item_chapter) {

	override fun onBind(data: MangaChapter) {
		textView_title.text = data.name
		textView_number.text = data.number.toString()
	}
}