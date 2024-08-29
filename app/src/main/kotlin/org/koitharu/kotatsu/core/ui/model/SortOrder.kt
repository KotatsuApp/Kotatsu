package org.koitharu.kotatsu.core.ui.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.SortDirection
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.model.SortOrder.ALPHABETICAL
import org.koitharu.kotatsu.parsers.model.SortOrder.ALPHABETICAL_DESC
import org.koitharu.kotatsu.parsers.model.SortOrder.NEWEST
import org.koitharu.kotatsu.parsers.model.SortOrder.NEWEST_ASC
import org.koitharu.kotatsu.parsers.model.SortOrder.POPULARITY
import org.koitharu.kotatsu.parsers.model.SortOrder.POPULARITY_ASC
import org.koitharu.kotatsu.parsers.model.SortOrder.RATING
import org.koitharu.kotatsu.parsers.model.SortOrder.RATING_ASC
import org.koitharu.kotatsu.parsers.model.SortOrder.UPDATED
import org.koitharu.kotatsu.parsers.model.SortOrder.UPDATED_ASC

@get:StringRes
val SortOrder.titleRes: Int
	get() = when (this) {
		UPDATED -> R.string.updated
		POPULARITY -> R.string.popular
		RATING -> R.string.by_rating
		NEWEST -> R.string.newest
		ALPHABETICAL -> R.string.by_name
		ALPHABETICAL_DESC -> R.string.by_name_reverse
		UPDATED_ASC -> R.string.updated_long_ago
		POPULARITY_ASC -> R.string.unpopular
		RATING_ASC -> R.string.low_rating
		NEWEST_ASC -> R.string.order_oldest
	}

val SortOrder.direction: SortDirection
	get() = when (this) {
		UPDATED_ASC,
		POPULARITY_ASC,
		RATING_ASC,
		NEWEST_ASC,
		ALPHABETICAL -> SortDirection.ASC

		UPDATED,
		POPULARITY,
		RATING,
		NEWEST,
		ALPHABETICAL_DESC -> SortDirection.DESC
	}
