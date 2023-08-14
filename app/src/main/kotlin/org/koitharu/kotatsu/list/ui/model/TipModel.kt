package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class TipModel(
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
}
