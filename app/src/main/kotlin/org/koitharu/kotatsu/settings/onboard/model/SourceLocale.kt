package org.koitharu.kotatsu.settings.onboard.model

import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import java.util.Locale

data class SourceLocale(
	val key: String?,
	val title: String?,
	val summary: String?,
	val isChecked: Boolean,
) : ListModel, Comparable<SourceLocale> {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is SourceLocale && key == other.key
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is SourceLocale && previousState.isChecked != isChecked) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}

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
