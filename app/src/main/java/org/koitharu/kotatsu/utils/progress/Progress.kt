package org.koitharu.kotatsu.utils.progress

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Progress(
	val value: Int,
	val total: Int
) : Parcelable, Comparable<Progress> {

	override fun compareTo(other: Progress): Int {
		if (this.total == other.total) {
			return this.value.compareTo(other.value)
		} else {
			TODO()
		}
	}
}