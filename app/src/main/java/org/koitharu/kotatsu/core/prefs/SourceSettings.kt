package org.koitharu.kotatsu.core.prefs

import android.content.Context
import org.koitharu.kotatsu.core.model.MangaSource

interface SourceSettings {

	fun getDomain(defaultValue: String): String

	fun isUseSsl(defaultValue: Boolean): Boolean

	private class PrefSourceSettings(context: Context, source: MangaSource) : SourceSettings {

		private val prefs = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

		override fun getDomain(defaultValue: String) = prefs.getString(KEY_DOMAIN, defaultValue)
			?.takeUnless(String::isBlank)
			?: defaultValue

		override fun isUseSsl(defaultValue: Boolean) = prefs.getBoolean(KEY_USE_SSL, defaultValue)
	}

	companion object {

		@JvmStatic
		operator fun invoke(context: Context, source: MangaSource): SourceSettings =
			PrefSourceSettings(context, source)

		const val KEY_DOMAIN = "domain"
		const val KEY_USE_SSL = "ssl"
	}
}