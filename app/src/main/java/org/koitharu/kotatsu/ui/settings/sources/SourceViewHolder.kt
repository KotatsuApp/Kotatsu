package org.koitharu.kotatsu.ui.settings.sources

import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_source_config.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder

class SourceViewHolder(parent: ViewGroup) :
	BaseViewHolder<MangaSource, Boolean>(parent, R.layout.item_source_config) {

	override fun onBind(data: MangaSource, extra: Boolean) {
		textView_title.text = data.title
		imageView_hidden.isChecked = extra
	}
}