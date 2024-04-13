package org.koitharu.kotatsu.list.ui.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface ListConfigSection : Parcelable {

	@Parcelize
	data object History : ListConfigSection

	@Parcelize
	data object General : ListConfigSection

	@Parcelize
	data class Favorites(
		val categoryId: Long,
	) : ListConfigSection

	@Parcelize
	data object Suggestions : ListConfigSection

	@Parcelize
	data object Updated : ListConfigSection
}
