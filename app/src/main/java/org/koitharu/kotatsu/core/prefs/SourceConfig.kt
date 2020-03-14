package org.koitharu.kotatsu.core.prefs

import android.content.Context
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource

class SourceConfig(context: Context, source: MangaSource) {

	private val prefs = context.getSharedPreferences(source.name, Context.MODE_PRIVATE)

	private val keyDomain = context.getString(R.string.key_parser_domain)

	fun getDomain(defaultValue: String) = prefs.getString(keyDomain, defaultValue) ?: defaultValue

}