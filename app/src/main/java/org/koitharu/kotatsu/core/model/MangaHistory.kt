package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class MangaHistory(
	val createdAt: Date,
	val updatedAt: Date,
	val chapterId: Long,
	val page: Int
) : Parcelable