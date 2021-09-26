package org.koitharu.kotatsu.settings.onboard.model

import java.util.*

data class SourceLocale(
	val key: String?,
	val title: String?,
	val isChecked: Boolean,
) : Comparable<SourceLocale> {

	override fun compareTo(other: SourceLocale): Int {
		return when {
			this === other -> 0
			key == Locale.getDefault().language -> -2
			key == null -> 1
			other.key == null -> -1
			else -> compareValues(title, other.title)
		}
	}
}
