package org.koitharu.kotatsu.scrobbling.ui.selector.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.ListModel

class ScrobblerHint(
	@DrawableRes val icon: Int,
	@StringRes val textPrimary: Int,
	@StringRes val textSecondary: Int,
	val error: Throwable?,
	@StringRes val actionStringRes: Int,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ScrobblerHint

		if (icon != other.icon) return false
		if (textPrimary != other.textPrimary) return false
		if (textSecondary != other.textSecondary) return false
		if (error != other.error) return false
		if (actionStringRes != other.actionStringRes) return false

		return true
	}

	override fun hashCode(): Int {
		var result = icon
		result = 31 * result + textPrimary
		result = 31 * result + textSecondary
		result = 31 * result + (error?.hashCode() ?: 0)
		result = 31 * result + actionStringRes
		return result
	}
}
