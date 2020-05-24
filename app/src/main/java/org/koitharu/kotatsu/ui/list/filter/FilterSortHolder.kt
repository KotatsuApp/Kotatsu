package org.koitharu.kotatsu.ui.list.filter

import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_checkable_single.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class FilterSortHolder(parent: ViewGroup) :
	BaseViewHolder<SortOrder, Boolean>(parent, R.layout.item_checkable_single) {

	override fun onBind(data: SortOrder, extra: Boolean) {
		radio.setText(data.titleRes)
		radio.isChecked = extra
	}
}