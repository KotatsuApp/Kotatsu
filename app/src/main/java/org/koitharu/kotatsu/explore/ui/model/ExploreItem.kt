package org.koitharu.kotatsu.explore.ui.model

import android.net.Uri
import androidx.annotation.StringRes
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface ExploreItem {

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
		val summary: String?,
	) : ExploreItem {

		val faviconUrl: Uri
			get() = Uri.fromParts("favicon", source.name, null)

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Source

			if (source != other.source) return false
			if (summary != other.summary) return false

			return true
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + summary.hashCode()
			return result
		}
	}

}