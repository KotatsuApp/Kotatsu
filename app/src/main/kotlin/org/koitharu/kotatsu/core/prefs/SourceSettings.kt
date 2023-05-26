package org.koitharu.kotatsu.core.prefs

import android.content.Context
import androidx.core.content.edit
import org.koitharu.kotatsu.core.util.ext.getEnumValue
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.putEnumValue
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.SortOrder

private const val KEY_SORT_ORDER = "sort_order"

class SourceSettings(context: Context, source: MangaSource) : MangaSourceConfig {

	private val prefs = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

	var defaultSortOrder: SortOrder?
		get() = prefs.getEnumValue(KEY_SORT_ORDER, SortOrder::class.java)
		set(value) = prefs.edit { putEnumValue(KEY_SORT_ORDER, value) }

	@Suppress("UNCHECKED_CAST")
	override fun <T> get(key: ConfigKey<T>): T {
		return when (key) {
			is ConfigKey.UserAgent -> prefs.getString(key.key, key.defaultValue).ifNullOrEmpty { key.defaultValue }
			is ConfigKey.Domain -> prefs.getString(key.key, key.defaultValue).ifNullOrEmpty { key.defaultValue }
			is ConfigKey.ShowSuspiciousContent -> prefs.getBoolean(key.key, key.defaultValue)
		} as T
	}

	operator fun <T> set(key: ConfigKey<T>, value: T) = prefs.edit {
		when (key) {
			is ConfigKey.Domain -> putString(key.key, value as String?)
			is ConfigKey.ShowSuspiciousContent -> putBoolean(key.key, value as Boolean)
			is ConfigKey.UserAgent -> putString(key.key, value as String?)
		}
	}
}
