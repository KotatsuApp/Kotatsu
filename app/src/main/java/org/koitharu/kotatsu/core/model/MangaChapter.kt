package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaChapter(
	val id: Long,
	val name: String,
	val number: Int,
	val url: String,
	val scanlator: String? = null,
	val date_upload: Long,
	val branch: String? = null,
	val source: MangaSource
) : Parcelable