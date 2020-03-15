package org.koitharu.kotatsu.utils.delegates.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LongPreferenceDelegate(private val key: String, private val defValue: Long) :
	ReadWriteProperty<SharedPreferences, Long> {

	override fun getValue(thisRef: SharedPreferences, property: KProperty<*>): Long {
		return thisRef.getLong(key, defValue)
	}

	override fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Long) {
		thisRef.edit {
			putLong(key, value)
		}
	}
}