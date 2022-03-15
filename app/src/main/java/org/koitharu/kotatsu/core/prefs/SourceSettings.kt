package org.koitharu.kotatsu.core.prefs

import android.content.Context
import org.koitharu.kotatsu.parsers.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaSource

class SourceSettings(context: Context, source: MangaSource) : MangaSourceConfig {

	private val prefs = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

	override fun getDomain(defaultValue: String) = prefs.getString(KEY_DOMAIN, defaultValue)
		?.takeUnless(String::isBlank)
		?: defaultValue

	override fun isSslEnabled(defaultValue: Boolean) = prefs.getBoolean(KEY_USE_SSL, defaultValue)

	companion object {

		const val KEY_DOMAIN = "domain"
		const val KEY_USE_SSL = "ssl"
		const val KEY_AUTH = "auth"
	}
}