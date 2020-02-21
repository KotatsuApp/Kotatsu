package org.koitharu.kotatsu.utils.delegates.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StringIntPreferenceDelegate(private val key: String, private val defValue: Int) :
	ReadWriteProperty<SharedPreferences, Int> {

	override fun getValue(thisRef: SharedPreferences, property: KProperty<*>): Int {
		return thisRef.getString(key, defValue.toString())?.toIntOrNull() ?: defValue
	}

	override fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Int) {
		thisRef.edit {
			putString(key, value.toString())
		}
	}
}