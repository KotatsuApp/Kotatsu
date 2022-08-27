package org.koitharu.kotatsu.tracker.domain.model

import java.util.*
import org.koitharu.kotatsu.parsers.model.Manga

data class TrackingLogItem(
	val id: Long,
	val manga: Manga,
	val chapters: List<String>,
	val createdAt: Date,
	val isNew: Boolean,
)
