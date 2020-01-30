package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MangaTag(
	val title: String,
	val key: String
) : Parcelable