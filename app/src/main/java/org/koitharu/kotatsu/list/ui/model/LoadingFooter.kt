package org.koitharu.kotatsu.list.ui.model

class LoadingFooter @JvmOverloads constructor(
	val key: Int = 0,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as LoadingFooter

		return key == other.key
	}

	override fun hashCode(): Int {
		return key
	}
}
