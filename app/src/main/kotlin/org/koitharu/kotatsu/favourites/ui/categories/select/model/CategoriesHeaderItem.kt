package org.koitharu.kotatsu.favourites.ui.categories.select.model

import org.koitharu.kotatsu.list.ui.model.ListModel

class CategoriesHeaderItem : ListModel {

	override fun equals(other: Any?): Boolean = other?.javaClass == CategoriesHeaderItem::class.java
}
