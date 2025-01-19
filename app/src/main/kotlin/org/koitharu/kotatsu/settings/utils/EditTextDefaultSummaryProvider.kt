package org.koitharu.kotatsu.settings.utils

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty

class EditTextDefaultSummaryProvider(
	private val defaultValue: String,
) : Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(
		preference: EditTextPreference,
	): CharSequence = preference.text.ifNullOrEmpty {
		preference.context.getString(R.string.default_s, defaultValue)
	}
}
