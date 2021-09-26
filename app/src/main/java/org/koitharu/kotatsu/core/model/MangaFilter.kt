package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaFilter(
	val sortOrder: SortOrder?,
	val tags: Set<MangaTag>,
) : Parcelable