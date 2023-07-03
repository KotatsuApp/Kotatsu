package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class ErrorState(
	val exception: Throwable,
	@DrawableRes val icon: Int,
	val canRetry: Boolean,
	@StringRes val buttonText: Int
) : ListModel {


	override fun areItemsTheSame(other: ListModel) = other is ErrorState
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ErrorState

		if (exception != other.exception) return false
		if (icon != other.icon) return false
		if (canRetry != other.canRetry) return false
		return buttonText == other.buttonText
	}

	override fun hashCode(): Int {
		var result = exception.hashCode()
		result = 31 * result + icon
		result = 31 * result + canRetry.hashCode()
		result = 31 * result + buttonText
		return result
	}
}
