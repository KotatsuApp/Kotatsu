package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes

class ErrorFooter(
	val exception: Throwable,
	@DrawableRes val icon: Int
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ErrorFooter

		if (exception != other.exception) return false
		return icon == other.icon
	}

	override fun hashCode(): Int {
		var result = exception.hashCode()
		result = 31 * result + icon
		return result
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ErrorFooter && exception == other.exception
	}
}
