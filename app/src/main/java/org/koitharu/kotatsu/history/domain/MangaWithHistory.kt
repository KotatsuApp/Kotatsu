package org.koitharu.kotatsu.history.domain

import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory

data class MangaWithHistory(
	val manga: Manga,
	val history: MangaHistory
)