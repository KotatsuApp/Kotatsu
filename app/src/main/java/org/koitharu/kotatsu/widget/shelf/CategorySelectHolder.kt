package org.koitharu.kotatsu.widget.shelf

import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_category_checkable.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.FavouriteCategory

class CategorySelectHolder(parent: ViewGroup) :
	BaseViewHolder<FavouriteCategory, Boolean>(parent, R.layout.item_category_checkable_single) {

	override fun onBind(data: FavouriteCategory, extra: Boolean) {
		checkedTextView.text = data.title
		checkedTextView.isChecked = extra
	}
}