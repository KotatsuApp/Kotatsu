package org.koitharu.kotatsu.list.ui.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.databinding.ItemCheckableSingleBinding

class FilterSortHolder(parent: ViewGroup) :
	BaseViewHolder<SortOrder, Boolean, ItemCheckableSingleBinding>(
		ItemCheckableSingleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
	) {

	override fun onBind(data: SortOrder, extra: Boolean) {
		binding.root.setText(data.titleRes)
		binding.root.isChecked = extra
	}
}