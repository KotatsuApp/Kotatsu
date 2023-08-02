package org.koitharu.kotatsu.list.ui.model

data class LoadingFooter @JvmOverloads constructor(
	val key: Int = 0,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is LoadingFooter && key == other.key
	}
}
