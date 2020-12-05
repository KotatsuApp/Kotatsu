package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes

data class ErrorState(
	val exception: Throwable,
	@DrawableRes val icon: Int,
	val canRetry: Boolean
) : ListModel