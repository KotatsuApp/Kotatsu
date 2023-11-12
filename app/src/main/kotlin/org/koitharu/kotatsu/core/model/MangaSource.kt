package org.koitharu.kotatsu.core.model

import android.content.Context
import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.toTitleCase
import java.util.Locale

fun MangaSource.getLocaleTitle(): String? {
	val lc = Locale(locale ?: return null)
	return lc.getDisplayLanguage(lc).toTitleCase(lc)
}

fun MangaSource(name: String): MangaSource {
	MangaSource.entries.forEach {
		if (it.name == name) return it
	}
	return MangaSource.DUMMY
}

fun MangaSource.isNsfw() = contentType == ContentType.HENTAI

@get:StringRes
val ContentType.titleResId
	get() = when (this) {
		ContentType.MANGA -> R.string.content_type_manga
		ContentType.HENTAI -> R.string.content_type_hentai
		ContentType.COMICS -> R.string.content_type_comics
		ContentType.OTHER -> R.string.content_type_other
	}

fun MangaSource.getSummary(context: Context): String {
	val type = context.getString(contentType.titleResId)
	val locale = getLocaleTitle() ?: context.getString(R.string.various_languages)
	return context.getString(R.string.source_summary_pattern, type, locale)
}
