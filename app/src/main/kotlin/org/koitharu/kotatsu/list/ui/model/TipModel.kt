package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class TipModel(
	val key: String,
	@StringRes val title: Int,
	@StringRes val text: Int,
	@DrawableRes val icon: Int,
	@StringRes val primaryButtonText: Int,
	@StringRes val secondaryButtonText: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is TipModel && other.key == key
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as TipModel

		if (key != other.key) return false
		if (title != other.title) return false
		if (text != other.text) return false
		if (icon != other.icon) return false
		if (primaryButtonText != other.primaryButtonText) return false
		return secondaryButtonText == other.secondaryButtonText
	}

	override fun hashCode(): Int {
		var result = key.hashCode()
		result = 31 * result + title
		result = 31 * result + text
		result = 31 * result + icon
		result = 31 * result + primaryButtonText
		result = 31 * result + secondaryButtonText
		return result
	}
}
