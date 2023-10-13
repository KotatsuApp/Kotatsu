package org.koitharu.kotatsu.list.domain

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.find
import java.util.EnumSet

enum class ListSortOrder(
	@StringRes val titleResId: Int,
) {

	UPDATED(R.string.updated),
	NEWEST(R.string.order_added),
	PROGRESS(R.string.progress),
	ALPHABETIC(R.string.by_name),
	RATING(R.string.by_rating),
	RELEVANCE(R.string.by_relevance),
	;

	fun isGroupingSupported() = this == UPDATED || this == NEWEST || this == PROGRESS

	companion object {

		val HISTORY = EnumSet.of(UPDATED, NEWEST, PROGRESS, ALPHABETIC)
		val FAVORITES = EnumSet.of(ALPHABETIC, NEWEST, RATING)
		val SUGGESTIONS = EnumSet.of(RELEVANCE)

		operator fun invoke(value: String, fallback: ListSortOrder) = entries.find(value) ?: fallback
	}
}
