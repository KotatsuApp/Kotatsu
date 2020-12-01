package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class FavouriteCategory(
	val id: Long,
	val title: String,
	val sortKey: Int,
	val createdAt: Date
) : Parcelable