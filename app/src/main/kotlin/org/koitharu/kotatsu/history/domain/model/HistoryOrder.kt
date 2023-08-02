package org.koitharu.kotatsu.history.domain.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class HistoryOrder(
	@StringRes val titleResId: Int,
) {

	UPDATED(R.string.updated),
	CREATED(R.string.order_added),
	PROGRESS(R.string.progress),
	ALPHABETIC(R.string.by_name);

	fun isGroupingSupported() = this == UPDATED || this == CREATED || this == PROGRESS
}
