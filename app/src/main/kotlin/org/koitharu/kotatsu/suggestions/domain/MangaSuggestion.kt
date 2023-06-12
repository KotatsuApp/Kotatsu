package org.koitharu.kotatsu.suggestions.domain

import androidx.annotation.FloatRange
import org.koitharu.kotatsu.parsers.model.Manga

data class MangaSuggestion(
	val manga: Manga,
	@FloatRange(from = 0.0, to = 1.0)
	val relevance: Float,
)