package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.list.ui.model.ListModel

class MangaCategoriesAdapter(
	clickListener: OnListItemClickListener<MangaCategoryItem>,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(mangaCategoryAD(clickListener))
			.addDelegate(categoriesHeaderAD())
	}
}
