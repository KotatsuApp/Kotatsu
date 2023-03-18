package org.koitharu.kotatsu.utils.progress

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Deprecated("Should be replaced with Float")
@Parcelize
data class Progress(
	val value: Int,
	val total: Int,
) : Parcelable, Comparable<Progress> {

	override fun compareTo(other: Progress): Int {
		return if (this.total == other.total) {
			this.value.compareTo(other.value)
		} else {
			this.part().compareTo(other.part())
		}
	}

	val isIndeterminate: Boolean
		get() = total <= 0

	private fun part() = if (isIndeterminate) -1.0 else value / total.toDouble()
}
