package org.koitharu.kotatsu.ui.settings.sources

import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_source_config.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class SourceViewHolder(parent: ViewGroup) :
	BaseViewHolder<MangaSource, Unit>(parent, R.layout.item_source_config) {

    override fun onBind(data: MangaSource, extra: Unit) {
        textView_title.text = data.title
    }
}