package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class ErrorState(
	val exception: Throwable,
	@DrawableRes val icon: Int,
	val canRetry: Boolean,
	@StringRes val buttonText: Int,
	@StringRes val secondaryButtonText: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel) = other is ErrorState
}
