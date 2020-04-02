package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class MangaTracking (
	val manga: Manga,
	val knownChaptersCount: Int,
	val lastChapterId: Long,
	val lastNotifiedChapterId: Long,
	val lastCheck: Date?
): Parcelable