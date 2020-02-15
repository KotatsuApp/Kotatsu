package org.koitharu.kotatsu.core.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class SortOrder(@StringRes val titleRes: Int) {
	UPDATED(R.string.updated),
	POPULARITY(R.string.popular),
	RATING(R.string.by_rating),
	NEWEST(R.string.newest),
	ALPHABETICAL(R.string.by_name)
}