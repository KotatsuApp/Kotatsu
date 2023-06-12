package org.koitharu.kotatsu.core.util.ext

import android.content.SharedPreferences
import androidx.preference.ListPreference

fun ListPreference.setDefaultValueCompat(defaultValue: String) {
	if (value == null) {
		value = defaultValue
	}
}

fun <E : Enum<E>> SharedPreferences.getEnumValue(key: String, enumClass: Class<E>): E? {
	val stringValue = getString(key, null) ?: return null
	return enumClass.enumConstants?.find {
		it.name == stringValue
	}
}

fun <E : Enum<E>> SharedPreferences.getEnumValue(key: String, defaultValue: E): E {
	return getEnumValue(key, defaultValue.javaClass) ?: defaultValue
}

fun <E : Enum<E>> SharedPreferences.Editor.putEnumValue(key: String, value: E?) {
	putString(key, value?.name)
}
