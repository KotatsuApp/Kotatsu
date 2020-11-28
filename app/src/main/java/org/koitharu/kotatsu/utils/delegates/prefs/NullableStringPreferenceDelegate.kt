package org.koitharu.kotatsu.utils.delegates.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class NullableStringPreferenceDelegate(private val key: String) :
	ReadWriteProperty<SharedPreferences, String?> {

	override fun getValue(thisRef: SharedPreferences, property: KProperty<*>): String? {
		return thisRef.getString(key, null)
	}

	override fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: String?) {
		thisRef.edit {
			putString(key, value)
		}
	}
}