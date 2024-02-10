package org.koitharu.kotatsu.settings.nav.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.prefs.NavItem
import org.koitharu.kotatsu.list.ui.model.ListModel

data class NavItemConfigModel(
	val item: NavItem,
	@StringRes val disabledHintResId: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is NavItemConfigModel && other.item == item
	}
}
