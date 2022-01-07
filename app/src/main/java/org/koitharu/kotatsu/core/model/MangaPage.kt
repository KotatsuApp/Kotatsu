package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaPage(
	val id: Long,
	val url: String,
	val referer: String,
	val preview: String?,
	val source: MangaSource,
) : Parcelable