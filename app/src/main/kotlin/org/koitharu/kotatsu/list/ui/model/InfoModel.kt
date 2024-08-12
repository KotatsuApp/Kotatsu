package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class InfoModel(
	val key: String,
	@StringRes val title: Int,
	@StringRes val text: Int,
	@DrawableRes val icon: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is InfoModel && other.key == key
	}
}
