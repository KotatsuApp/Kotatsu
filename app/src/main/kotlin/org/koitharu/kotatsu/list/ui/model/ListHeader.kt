package org.koitharu.kotatsu.list.ui.model

import android.content.Context
import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo

data class ListHeader private constructor(
	private val text: CharSequence? = null,
	@StringRes private val textRes: Int = 0,
	private val dateTimeAgo: DateTimeAgo? = null,
	@StringRes val buttonTextRes: Int = 0,
	val payload: Any? = null,
) : ListModel {

	constructor(
		text: CharSequence,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
	) : this(text, 0, null, buttonTextRes, payload)

	constructor(
		@StringRes textRes: Int,
		@StringRes buttonTextRes: Int = 0
	) : this(null, textRes, null, buttonTextRes)

	constructor(dateTimeAgo: DateTimeAgo) : this(null, dateTimeAgo = dateTimeAgo)

	fun getText(context: Context): CharSequence? = when {
		text != null -> text
		textRes != 0 -> context.getString(textRes)
		else -> dateTimeAgo?.format(context.resources)
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ListHeader && text == other.text && dateTimeAgo == other.dateTimeAgo && textRes == other.textRes
	}
}
