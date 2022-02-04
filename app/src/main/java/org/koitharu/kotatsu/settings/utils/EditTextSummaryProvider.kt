package org.koitharu.kotatsu.settings.utils

import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.Preference

class EditTextSummaryProvider(@StringRes private val emptySummaryId: Int) :
	Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(preference: EditTextPreference): CharSequence {
		val text = preference.text
		return if (text.isNullOrEmpty()) {
			preference.context.getString(emptySummaryId)
		} else {
			text
		}
	}
}