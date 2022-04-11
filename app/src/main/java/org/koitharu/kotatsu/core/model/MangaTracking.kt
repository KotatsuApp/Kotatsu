package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.*

data class MangaTracking(
	val manga: Manga,
	val knownChaptersCount: Int,
	val lastChapterId: Long,
	val lastNotifiedChapterId: Long,
	val lastCheck: Date?
)