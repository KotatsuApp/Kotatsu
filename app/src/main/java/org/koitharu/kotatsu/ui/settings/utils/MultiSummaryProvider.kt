package org.koitharu.kotatsu.ui.settings.utils

import android.annotation.SuppressLint
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference

class MultiSummaryProvider : Preference.SummaryProvider<MultiSelectListPreference> {

	@SuppressLint("PrivateResource")
	override fun provideSummary(preference: MultiSelectListPreference): CharSequence {
		val values = preference.values
		return if (values.isEmpty()) {
			return preference.context.getString(androidx.preference.R.string.not_set)
		} else {
			values.joinToString(", ") {
				preference.entries[preference.findIndexOfValue(it)]
			}
		}
	}
}