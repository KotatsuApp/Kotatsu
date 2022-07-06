package org.koitharu.kotatsu.core.prefs

import android.content.Context
import androidx.core.content.edit
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.ext.getEnumValue
import org.koitharu.kotatsu.utils.ext.ifNullOrEmpty
import org.koitharu.kotatsu.utils.ext.putEnumValue

private const val KEY_SORT_ORDER = "sort_order"

class SourceSettings(context: Context, source: MangaSource) : MangaSourceConfig {

	private val prefs = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

	var defaultSortOrder: SortOrder?
		get() = prefs.getEnumValue(KEY_SORT_ORDER, SortOrder::class.java)
		set(value) = prefs.edit { putEnumValue(KEY_SORT_ORDER, value) }

	@Suppress("UNCHECKED_CAST")
	override fun <T> get(key: ConfigKey<T>): T {
		return when (key) {
			is ConfigKey.Domain -> prefs.getString(key.key, key.defaultValue).ifNullOrEmpty { key.defaultValue }
		} as T
	}

	operator fun <T : Any> set(key: ConfigKey<T>, value: T?) {
		val editor = prefs.edit()
		when (key) {
			is ConfigKey.Domain -> if (value == null) {
				editor.remove(key.key)
			} else {
				editor.putString(key.key, value as String)
			}
		}
		editor.apply()
	}
}