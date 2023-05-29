package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class MangaHistory(
	val createdAt: Date,
	val updatedAt: Date,
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
	val percent: Float,
) : Parcelable