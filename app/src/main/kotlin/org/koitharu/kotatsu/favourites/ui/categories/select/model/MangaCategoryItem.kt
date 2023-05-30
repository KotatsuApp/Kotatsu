package org.koitharu.kotatsu.favourites.ui.categories.select.model

import org.koitharu.kotatsu.list.ui.model.ListModel

data class MangaCategoryItem(
	val id: Long,
	val name: String,
	val isChecked: Boolean,
) : ListModel
