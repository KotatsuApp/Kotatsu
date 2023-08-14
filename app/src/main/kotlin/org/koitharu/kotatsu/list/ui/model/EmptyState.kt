package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class EmptyState(
	@DrawableRes val icon: Int,
	@StringRes val textPrimary: Int,
	@StringRes val textSecondary: Int,
	@StringRes val actionStringRes: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is EmptyState
	}
}
