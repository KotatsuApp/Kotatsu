package org.koitharu.kotatsu.utils.delegates.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class IntPreferenceDelegate(private val key: String, private val defaultValue: Int) :
	ReadWriteProperty<SharedPreferences, Int> {

	override fun getValue(thisRef: SharedPreferences, property: KProperty<*>): Int {
		return thisRef.getInt(key, defaultValue)
	}

	override fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Int) {
		thisRef.edit {
			putInt(key, value)
		}
	}
}