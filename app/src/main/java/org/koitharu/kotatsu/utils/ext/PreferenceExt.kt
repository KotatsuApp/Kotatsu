package org.koitharu.kotatsu.utils.ext

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference

fun ListPreference.bindSummary(listener: (String) -> Boolean = { true }) {
	summary = entries.getOrNull(findIndexOfValue(value))
	this.setOnPreferenceChangeListener { preference, newValue ->
		newValue as String
		preference as ListPreference
		val res = listener(newValue)
		if (res) {
			preference.summary = preference.entries.getOrNull(preference.findIndexOfValue(newValue))
		}
		res
	}
}

fun EditTextPreference.bindSummary(
	formatter: (String) -> String = { it },
	listener: (String) -> Boolean = { true }
) {
	summary = formatter(text)
	this.setOnPreferenceChangeListener { preference, newValue ->
		newValue as String
		preference as EditTextPreference
		val res = listener(newValue)
		if (res) {
			preference.summary = formatter(newValue)
		}
		res
	}
}