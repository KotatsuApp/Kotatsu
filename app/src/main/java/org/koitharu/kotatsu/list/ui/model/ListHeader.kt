package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.parsers.model.SortOrder

@Deprecated("")
data class ListHeader(
	val text: CharSequence?,
	@StringRes val textRes: Int,
	val sortOrder: SortOrder?,
) : ListModel