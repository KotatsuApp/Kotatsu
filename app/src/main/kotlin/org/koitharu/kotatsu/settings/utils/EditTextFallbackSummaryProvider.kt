package org.koitharu.kotatsu.settings.utils

import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty

class EditTextFallbackSummaryProvider(
	@StringRes private val fallbackResId: Int,
) : Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(
		preference: EditTextPreference,
	): CharSequence = preference.text.ifNullOrEmpty {
		preference.context.getString(fallbackResId)
	}
}
