package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import java.util.*
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.parsers.model.SortOrder

@Parcelize
data class FavouriteCategory(
	val id: Long,
	val title: String,
	val sortKey: Int,
	val order: SortOrder,
	val createdAt: Date,
	val isTrackingEnabled: Boolean,
) : Parcelable