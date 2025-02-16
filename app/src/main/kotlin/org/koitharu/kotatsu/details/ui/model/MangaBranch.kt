package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import java.util.Locale

data class MangaBranch(
	val name: String?,
	val count: Int,
	val isSelected: Boolean,
	val isCurrent: Boolean,
) : ListModel {

	val locale: Locale? by lazy(::findAppropriateLocale)

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaBranch && other.name == name
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MangaBranch && previousState.isSelected != isSelected) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}

	override fun toString(): String {
		return "$name: $count"
	}

	private fun findAppropriateLocale(): Locale? {
		if (name.isNullOrEmpty()) {
			return null
		}
		return Locale.getAvailableLocales().find { lc ->
			name.contains(lc.getDisplayName(lc), ignoreCase = true) ||
			name.contains(lc.getDisplayName(Locale.ENGLISH), ignoreCase = true) ||
				name.contains(lc.getDisplayLanguage(lc), ignoreCase = true) ||
				name.contains(lc.getDisplayLanguage(Locale.ENGLISH), ignoreCase = true)
		}
	}
}
