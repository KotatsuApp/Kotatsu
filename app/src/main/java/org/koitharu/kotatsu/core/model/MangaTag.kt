package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaTag(
	val title: String,
	val key: String,
	val source: MangaSource
) : Parcelable