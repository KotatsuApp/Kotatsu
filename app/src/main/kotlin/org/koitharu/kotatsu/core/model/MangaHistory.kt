package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
data class MangaHistory(
	val createdAt: Instant,
	val updatedAt: Instant,
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
	val percent: Float,
) : Parcelable
