package org.koitharu.kotatsu.settings.sources

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.databinding.ItemSourceConfigBinding

class SourceViewHolder(parent: ViewGroup) :
	BaseViewHolder<MangaSource, Boolean, ItemSourceConfigBinding>(
		ItemSourceConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
	) {

	override fun onBind(data: MangaSource, extra: Boolean) {
		binding.textViewTitle.text = data.title
		binding.imageViewHidden.isChecked = extra
	}
}