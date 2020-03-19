package org.koitharu.kotatsu.utils.delegates.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StringSetPreferenceDelegate(private val key: String, private val defValue: Set<String> = emptySet()) :
	ReadWriteProperty<SharedPreferences, Set<String>> {

	override fun getValue(thisRef: SharedPreferences, property: KProperty<*>): Set<String> {
		return thisRef.getStringSet(key, defValue) ?: defValue
	}

	override fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Set<String>) {
		thisRef.edit {
			putStringSet(key, value)
		}
	}
}