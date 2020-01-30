package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MangaChapter(
	val id: Long,
	val name: String,
	val number: Int,
	val url: String,
	val source: MangaSource
) : Parcelable