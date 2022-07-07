package org.koitharu.kotatsu.explore.ui.model

import android.net.Uri
import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface ExploreItem : ListModel {

	object Buttons : ExploreItem

	class Header(
		@StringRes val titleResId: Int,
	) : ExploreItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as Header
			return titleResId == other.titleResId
		}

		override fun hashCode(): Int = titleResId
	}

	class Source(
		val source: MangaSource,
	) : ExploreItem {

		val faviconUrl: Uri
			get() = Uri.fromParts("favicon", source.name, null)

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Source

			if (source != other.source) return false

			return true
		}

		override fun hashCode(): Int {
			return source.hashCode()
		}
	}

}