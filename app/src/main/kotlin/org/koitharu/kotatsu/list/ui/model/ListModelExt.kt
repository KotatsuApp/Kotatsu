package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.util.ext.getDisplayIcon
import org.koitharu.kotatsu.core.util.ext.ifZero

fun Throwable.toErrorState(canRetry: Boolean = true, @StringRes secondaryAction: Int = 0) = ErrorState(
	exception = this,
	icon = getDisplayIcon(),
	canRetry = canRetry,
	buttonText = ExceptionResolver.getResolveStringId(this).ifZero { R.string.try_again },
	secondaryButtonText = secondaryAction,
)

fun Throwable.toErrorFooter() = ErrorFooter(
	exception = this,
)

operator fun ListModel.plus(list: List<ListModel>): List<ListModel> {
	val result = ArrayList<ListModel>(list.size + 1)
	result.add(this)
	result.addAll(list)
	return result
}

operator fun ListModel.plus(other: ListModel): List<ListModel> = listOf(this, other)
