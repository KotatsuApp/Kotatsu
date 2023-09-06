package org.koitharu.kotatsu.settings.nav.model

import org.koitharu.kotatsu.list.ui.model.ListModel

data class NavItemAddModel(
	val canAdd: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean = other is NavItemAddModel
}
