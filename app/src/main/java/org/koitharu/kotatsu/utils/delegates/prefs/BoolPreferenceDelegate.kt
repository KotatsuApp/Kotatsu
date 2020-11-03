package org.koitharu.kotatsu.utils.delegates.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BoolPreferenceDelegate(private val key: String, private val defaultValue: Boolean) :
	ReadWriteProperty<SharedPreferences, Boolean> {

	override fun getValue(thisRef: SharedPreferences, property: KProperty<*>): Boolean {
		return thisRef.getBoolean(key, defaultValue)
	}

	override fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Boolean) {
		thisRef.edit {
			putBoolean(key, value)
		}
	}
}