package org.koitharu.kotatsu.scrobbling.common.ui.selector.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.ListModel

data class ScrobblerHint(
	@DrawableRes val icon: Int,
	@StringRes val textPrimary: Int,
	@StringRes val textSecondary: Int,
	val error: Throwable?,
	@StringRes val actionStringRes: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ScrobblerHint && other.textPrimary == textPrimary
	}
}
