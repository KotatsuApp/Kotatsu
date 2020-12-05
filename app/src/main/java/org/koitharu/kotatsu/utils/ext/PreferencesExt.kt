package org.koitharu.kotatsu.utils.ext

import androidx.preference.ListPreference

fun ListPreference.setDefaultValueCompat(defaultValue: String) {
	if (value == null) {
		value = defaultValue
	}
}