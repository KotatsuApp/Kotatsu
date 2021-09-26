package org.koitharu.kotatsu.list.ui.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.databinding.ItemCheckableMultipleBinding

class FilterTagHolder(parent: ViewGroup) :
	BaseViewHolder<MangaTag, Boolean, ItemCheckableMultipleBinding>(
		ItemCheckableMultipleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
	) {

	val isChecked: Boolean
		get() = binding.root.isChecked

	override fun onBind(data: MangaTag, extra: Boolean) {
		binding.root.text = data.title
		binding.root.isChecked = extra
	}
}