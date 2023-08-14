package org.koitharu.kotatsu.settings.utils

import android.text.TextUtils
import androidx.preference.EditTextPreference
import androidx.preference.Preference

class PasswordSummaryProvider : Preference.SummaryProvider<EditTextPreference> {

	private val delegate = EditTextPreference.SimpleSummaryProvider.getInstance()

	override fun provideSummary(preference: EditTextPreference): CharSequence? {
		val summary = delegate.provideSummary(preference)
		return if (summary != null && !TextUtils.isEmpty(preference.text)) {
			String(CharArray(summary.length) { '\u2022' })
		} else {
			summary
		}
	}
}
