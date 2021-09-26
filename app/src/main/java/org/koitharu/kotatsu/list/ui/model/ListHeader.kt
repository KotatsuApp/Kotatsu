package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.StringRes

data class ListHeader(
	val text: CharSequence?,
	@StringRes val textRes: Int,
) : ListModel