package org.koitharu.kotatsu.settings.utils

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import org.koitharu.kotatsu.R

class EditTextDefaultSummaryProvider(
	private val defaultValue: String
) : Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(preference: EditTextPreference): CharSequence {
		return if (preference.text.isNullOrEmpty()) {
			preference.context.getString(R.string.default_s, defaultValue)
		} else {
			preference.text
		}
	}
}