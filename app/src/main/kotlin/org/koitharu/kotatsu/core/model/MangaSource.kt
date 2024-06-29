package org.koitharu.kotatsu.core.model

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import androidx.annotation.StringRes
import androidx.core.text.inSpans
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getDisplayName
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.toLocale
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import com.google.android.material.R as materialR

data object LocalMangaSource : MangaSource {
	override val name = "LOCAL"
}

data object UnknownMangaSource : MangaSource {
	override val name = "UNKNOWN"
}

fun MangaSource(name: String?): MangaSource {
	when (name) {
		null,
		UnknownMangaSource.name -> UnknownMangaSource

		LocalMangaSource.name -> LocalMangaSource
	}
	MangaParserSource.entries.forEach {
		if (it.name == name) return it
	}
	return UnknownMangaSource
}

fun MangaSource.isNsfw() = when (this) {
	is MangaParserSource -> contentType == ContentType.HENTAI
	else -> false
}

@get:StringRes
val ContentType.titleResId
	get() = when (this) {
		ContentType.MANGA -> R.string.content_type_manga
		ContentType.HENTAI -> R.string.content_type_hentai
		ContentType.COMICS -> R.string.content_type_comics
		ContentType.OTHER -> R.string.content_type_other
	}

fun MangaSource.getSummary(context: Context): String? = when (this) {
	is MangaParserSource -> {
		val type = context.getString(contentType.titleResId)
		val locale = locale.toLocale().getDisplayName(context)
		context.getString(R.string.source_summary_pattern, type, locale)
	}

	else -> null
}

fun MangaSource.getTitle(context: Context): String = when (this) {
	is MangaParserSource -> title
	LocalMangaSource -> context.getString(R.string.local_storage)
	else -> context.getString(R.string.unknown)
}

fun SpannableStringBuilder.appendNsfwLabel(context: Context) = inSpans(
	ForegroundColorSpan(context.getThemeColor(materialR.attr.colorError, Color.RED)),
	RelativeSizeSpan(0.74f),
	SuperscriptSpan(),
) {
	append(context.getString(R.string.nsfw))
}
