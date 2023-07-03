package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class EmptyState(
	@DrawableRes val icon: Int,
	@StringRes val textPrimary: Int,
	@StringRes val textSecondary: Int,
	@StringRes val actionStringRes: Int,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as EmptyState

		if (icon != other.icon) return false
		if (textPrimary != other.textPrimary) return false
		if (textSecondary != other.textSecondary) return false
		return actionStringRes == other.actionStringRes
	}

	override fun hashCode(): Int {
		var result = icon
		result = 31 * result + textPrimary
		result = 31 * result + textSecondary
		result = 31 * result + actionStringRes
		return result
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is EmptyState
	}
}
