package org.koitharu.kotatsu.core.util.ext

import android.content.SharedPreferences
import androidx.collection.ArraySet
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import org.json.JSONArray

fun ListPreference.setDefaultValueCompat(defaultValue: String) {
	if (value == null) {
		value = defaultValue
	}
}

fun MultiSelectListPreference.setDefaultValueCompat(defaultValue: Set<String>) {
	setDefaultValue(defaultValue) // FIXME not working
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

fun SharedPreferences.observeChanges(): Flow<String?> = callbackFlow {
	val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		trySendBlocking(key)
	}
	registerOnSharedPreferenceChangeListener(listener)
	awaitClose {
		unregisterOnSharedPreferenceChangeListener(listener)
	}
}

fun <T> SharedPreferences.observe(key: String, valueProducer: suspend () -> T): Flow<T> = flow {
	emit(valueProducer())
	observeChanges().collect { upstreamKey ->
		if (upstreamKey == key) {
			emit(valueProducer())
		}
	}
}.distinctUntilChanged()

fun SharedPreferences.Editor.putAll(values: Map<String, *>) {
	values.forEach { e ->
		when (val v = e.value) {
			is Boolean -> putBoolean(e.key, v)
			is Int -> putInt(e.key, v)
			is Long -> putLong(e.key, v)
			is Float -> putFloat(e.key, v)
			is String -> putString(e.key, v)
			is JSONArray -> putStringSet(e.key, v.toStringSet())
		}
	}
}

private fun JSONArray.toStringSet(): Set<String> {
	val len = length()
	val result = ArraySet<String>(len)
	for (i in 0 until len) {
		result.add(getString(i))
	}
	return result
}
