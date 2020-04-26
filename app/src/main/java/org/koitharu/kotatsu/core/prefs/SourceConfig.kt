package org.koitharu.kotatsu.core.prefs

import android.content.Context
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource

interface SourceConfig {

	fun getDomain(defaultValue: String): String

	fun isUseSsl(defaultValue: Boolean): Boolean

	private class PrefSourceConfig(context: Context, source: MangaSource) : SourceConfig {

		private val prefs = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

		private val keyDomain = context.getString(R.string.key_parser_domain)
		private val keySsl = context.getString(R.string.key_parser_ssl)

		override fun getDomain(defaultValue: String) = prefs.getString(keyDomain, defaultValue)
			?.takeUnless(String::isBlank)
			?: defaultValue

		override fun isUseSsl(defaultValue: Boolean) = prefs.getBoolean(keySsl, defaultValue)
	}

	companion object {

		@JvmStatic
		operator fun invoke(context: Context, source: MangaSource): SourceConfig =
			PrefSourceConfig(context, source)
	}
}