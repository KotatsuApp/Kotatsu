package org.koitharu.kotatsu.filter.ui.model

data class FilterProperty<T>(
	val availableItems: List<T>,
	val selectedItems: Set<T>,
	val isLoading: Boolean,
	val error: Throwable?,
) {

	fun isEmpty(): Boolean = availableItems.isEmpty()
}
