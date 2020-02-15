package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MangaFilter(
	val sortOrder: SortOrder,
	val tag: MangaTag?
) : Parcelable