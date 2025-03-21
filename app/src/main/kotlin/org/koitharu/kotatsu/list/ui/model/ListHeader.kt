package org.koitharu.kotatsu.list.ui.model

import android.content.Context
import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.model.getLocalizedTitle
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import org.koitharu.kotatsu.parsers.model.MangaChapter

data class ListHeader private constructor(
	private val textRaw: Any,
	@StringRes val buttonTextRes: Int,
	val payload: Any?,
	val badge: String?,
) : ListModel {

	constructor(
		text: CharSequence,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
		badge: String? = null,
	) : this(textRaw = text, buttonTextRes, payload, badge)

	constructor(
		@StringRes textRes: Int,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
		badge: String? = null,
	) : this(textRaw = textRes, buttonTextRes, payload, badge)

	constructor(
		chapter: MangaChapter,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
		badge: String? = null,
	) : this(textRaw = chapter, buttonTextRes, payload, badge)

	constructor(
		dateTimeAgo: DateTimeAgo,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
		badge: String? = null,
	) : this(textRaw = dateTimeAgo, buttonTextRes, payload, badge)

	fun getText(context: Context): CharSequence? = when (textRaw) {
		is CharSequence -> textRaw
		is Int -> if (textRaw != 0) context.getString(textRaw) else null
		is DateTimeAgo -> textRaw.format(context)
		is MangaChapter -> textRaw.getLocalizedTitle(context.resources)
		else -> null
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ListHeader && textRaw == other.textRaw
	}
}
