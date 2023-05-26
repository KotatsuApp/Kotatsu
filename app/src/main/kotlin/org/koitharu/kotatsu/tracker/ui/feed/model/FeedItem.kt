package org.koitharu.kotatsu.tracker.ui.feed.model

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

data class FeedItem(
	val id: Long,
	val imageUrl: String,
	val title: String,
	val manga: Manga,
	val count: Int,
	val isNew: Boolean,
) : ListModel
