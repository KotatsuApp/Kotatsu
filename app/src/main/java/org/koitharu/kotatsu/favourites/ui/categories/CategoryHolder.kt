package org.koitharu.kotatsu.favourites.ui.categories

import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_category.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.FavouriteCategory

class CategoryHolder(parent: ViewGroup) :
	BaseViewHolder<FavouriteCategory, Unit>(parent, R.layout.item_category) {

	override fun onBind(data: FavouriteCategory, extra: Unit) {
		textView_title.text = data.title
	}
}