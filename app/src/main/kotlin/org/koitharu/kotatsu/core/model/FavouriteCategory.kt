package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.*

@Parcelize
data class FavouriteCategory(
	val id: Long,
	val title: String,
	val sortKey: Int,
	val order: SortOrder,
	val createdAt: Date,
	val isTrackingEnabled: Boolean,
	val isVisibleInLibrary: Boolean,
) : Parcelable