package org.koitharu.kotatsu.history.domain.model

import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.parsers.model.Manga

data class MangaWithHistory(
	val manga: Manga,
	val history: MangaHistory
)
