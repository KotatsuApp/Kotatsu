package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaPage(
	val id: Long,
	val url: String,
	val preview: String? = null,
	val source: MangaSource
) : Parcelable