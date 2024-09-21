package org.koitharu.kotatsu.core.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.model.SortOrder

@Deprecated("")
enum class GenericSortOrder(
	@StringRes val titleResId: Int,
	val ascending: SortOrder,
	val descending: SortOrder,
) {

	UPDATED(R.string.updated, SortOrder.UPDATED_ASC, SortOrder.UPDATED),
	RATING(R.string.by_rating, SortOrder.RATING_ASC, SortOrder.RATING),
	POPULARITY(R.string.popularity, SortOrder.POPULARITY_ASC, SortOrder.POPULARITY),
	DATE(R.string.by_date, SortOrder.NEWEST_ASC, SortOrder.NEWEST),
	NAME(R.string.by_name, SortOrder.ALPHABETICAL, SortOrder.ALPHABETICAL_DESC),
	;

	operator fun get(direction: SortDirection): SortOrder = when (direction) {
		SortDirection.ASC -> ascending
		SortDirection.DESC -> descending
	}

	companion object {

		fun of(order: SortOrder): GenericSortOrder = entries.first { e ->
			e.ascending == order || e.descending == order
		}
	}
}
