package org.koitharu.kotatsu.settings.utils

import androidx.annotation.StringRes
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference

class MultiSummaryProvider(@StringRes private val emptySummaryId: Int) :
	Preference.SummaryProvider<MultiSelectListPreference> {

	override fun provideSummary(preference: MultiSelectListPreference): CharSequence {
		val values = preference.values
		return if (values.isEmpty()) {
			return preference.context.getString(emptySummaryId)
		} else {
			values.joinToString(", ") {
				preference.entries.getOrNull(preference.findIndexOfValue(it))
					?: preference.context.getString(androidx.preference.R.string.not_set)
			}
		}
	}
}
