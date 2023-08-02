package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes

data class ErrorFooter(
	val exception: Throwable,
	@DrawableRes val icon: Int
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ErrorFooter && exception == other.exception
	}
}
