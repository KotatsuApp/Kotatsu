package org.koitharu.kotatsu.list.ui.model

import android.content.Context
import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo

@Suppress("DataClassPrivateConstructor")
data class ListHeader private constructor(
	private val text: CharSequence?,
	@StringRes private val textRes: Int,
	private val dateTimeAgo: DateTimeAgo?,
	@StringRes val buttonTextRes: Int,
	val payload: Any?,
) : ListModel {

	constructor(
		text: CharSequence,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
	) : this(text, 0, null, buttonTextRes, payload)

	constructor(
		@StringRes textRes: Int,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
	) : this(null, textRes, null, buttonTextRes, payload)

	constructor(
		dateTimeAgo: DateTimeAgo,
		@StringRes buttonTextRes: Int = 0,
		payload: Any? = null,
	) : this(null, 0, dateTimeAgo, buttonTextRes, payload)

	fun getText(context: Context): CharSequence? = when {
		text != null -> text
		textRes != 0 -> context.getString(textRes)
		else -> dateTimeAgo?.format(context.resources)
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ListHeader && text == other.text && dateTimeAgo == other.dateTimeAgo && textRes == other.textRes
	}
}
