package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaChapter(
	val id: Long,
	val name: String,
	val number: Int,
	val url: String,
	val scanlator: String?,
	val uploadDate: Long,
	val branch: String?,
	val source: MangaSource,
) : Parcelable, Comparable<MangaChapter> {

	override fun compareTo(other: MangaChapter): Int {
		return number.compareTo(other.number)
	}
}