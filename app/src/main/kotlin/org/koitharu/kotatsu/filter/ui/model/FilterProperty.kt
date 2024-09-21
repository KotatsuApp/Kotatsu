package org.koitharu.kotatsu.filter.ui.model

data class FilterProperty<out T>(
	val availableItems: List<T>,
	val selectedItems: Set<T>,
	val isLoading: Boolean,
	val error: Throwable?,
) {

	constructor(
		availableItems: List<T>,
		selectedItems: Set<T>,
	) : this(
		availableItems = availableItems,
		selectedItems = selectedItems,
		isLoading = false,
		error = null,
	)

	constructor(
		availableItems: List<T>,
		selectedItem: T,
	) : this(
		availableItems = availableItems,
		selectedItems = setOf(selectedItem),
		isLoading = false,
		error = null,
	)

	fun isEmpty(): Boolean = availableItems.isEmpty()

	fun isEmptyAndSuccess(): Boolean = availableItems.isEmpty() && error == null

	companion object {

		val LOADING = FilterProperty<Nothing>(
			availableItems = emptyList(),
			selectedItems = emptySet(),
			isLoading = true,
			error = null,
		)

		val EMPTY = FilterProperty<Nothing>(
			availableItems = emptyList(),
			selectedItems = emptySet(),
		)

		fun error(error: Throwable) = FilterProperty<Nothing>(
			availableItems = emptyList(),
			selectedItems = emptySet(),
			isLoading = false,
			error = error,
		)
	}
}
