package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import java.util.*
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.parsers.model.Manga

data class MangaTracking(
	val manga: Manga,
	val knownChaptersCount: Int,
	val lastChapterId: Long,
	val lastNotifiedChapterId: Long,
	val lastCheck: Date?
)