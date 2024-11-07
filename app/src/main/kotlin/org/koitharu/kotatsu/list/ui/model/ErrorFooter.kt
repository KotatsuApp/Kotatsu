package org.koitharu.kotatsu.list.ui.model

data class ErrorFooter(
	val exception: Throwable,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ErrorFooter && exception == other.exception
	}
}
