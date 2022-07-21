package org.koitharu.kotatsu.list.ui.model

import android.content.Context
import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.ui.DateTimeAgo

class ListHeader private constructor(
	val text: CharSequence?,
	@StringRes val textRes: Int,
	val dateTimeAgo: DateTimeAgo?,
	@StringRes val buttonTextRes: Int,
	val payload: Any?,
) : ListModel {

	constructor(
		text: CharSequence,
		@StringRes buttonTextRes: Int,
		payload: Any?,
	) : this(text, 0, null, buttonTextRes, payload)

	constructor(
		@StringRes textRes: Int,
		@StringRes buttonTextRes: Int,
		payload: Any?,
	) : this(null, textRes, null, buttonTextRes, payload)

	constructor(
		dateTimeAgo: DateTimeAgo,
		@StringRes buttonTextRes: Int,
		payload: Any?,
	) : this(null, 0, dateTimeAgo, buttonTextRes, payload)

	fun getText(context: Context): CharSequence? = when {
		text != null -> text
		textRes != 0 -> context.getString(textRes)
		else -> dateTimeAgo?.format(context.resources)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ListHeader

		if (text != other.text) return false
		if (textRes != other.textRes) return false
		if (dateTimeAgo != other.dateTimeAgo) return false
		if (buttonTextRes != other.buttonTextRes) return false
		if (payload != other.payload) return false

		return true
	}

	override fun hashCode(): Int {
		var result = text?.hashCode() ?: 0
		result = 31 * result + textRes
		result = 31 * result + (dateTimeAgo?.hashCode() ?: 0)
		result = 31 * result + buttonTextRes
		result = 31 * result + (payload?.hashCode() ?: 0)
		return result
	}
}
