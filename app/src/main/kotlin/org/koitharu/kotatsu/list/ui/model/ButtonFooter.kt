package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.StringRes

data class ButtonFooter(
	@StringRes val textResId: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ButtonFooter && textResId == other.textResId
	}
}
