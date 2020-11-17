package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class TrackingLogItem(
	val id: Long,
	val manga: Manga,
	val chapters: List<String>,
	val createdAt: Date
) : Parcelable