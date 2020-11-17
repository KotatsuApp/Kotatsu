package org.koitharu.kotatsu.list.ui.filter

import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_checkable_single.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.SortOrder

class FilterSortHolder(parent: ViewGroup) :
	BaseViewHolder<SortOrder, Boolean>(parent, R.layout.item_checkable_single) {

	override fun onBind(data: SortOrder, extra: Boolean) {
		radio.setText(data.titleRes)
		radio.isChecked = extra
	}
}