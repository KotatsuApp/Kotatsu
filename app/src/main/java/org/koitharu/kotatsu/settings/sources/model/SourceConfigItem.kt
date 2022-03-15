package org.koitharu.kotatsu.settings.sources.model

import android.net.Uri
import androidx.annotation.StringRes
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface SourceConfigItem {

	class Header(
		@StringRes val titleResId: Int,
	) : SourceConfigItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as Header
			return titleResId == other.titleResId
		}

		override fun hashCode(): Int = titleResId
	}

	class LocaleGroup(
		val localeId: String?,
		val title: String?,
		val isExpanded: Boolean,
	) : SourceConfigItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as LocaleGroup

			if (localeId != other.localeId) return false
			if (title != other.title) return false
			if (isExpanded != other.isExpanded) return false

			return true
		}

		override fun hashCode(): Int {
			var result = localeId?.hashCode() ?: 0
			result = 31 * result + (title?.hashCode() ?: 0)
			result = 31 * result + isExpanded.hashCode()
			return result
		}
	}

	class SourceItem(
		val source: MangaSource,
		val isEnabled: Boolean,
		val isDraggable: Boolean,
	) : SourceConfigItem {

		val faviconUrl: Uri
			get() = Uri.fromParts("favicon", source.name, null)

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as SourceItem

			if (source != other.source) return false
			if (isEnabled != other.isEnabled) return false
			if (isDraggable != other.isDraggable) return false

			return true
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + isEnabled.hashCode()
			result = 31 * result + isDraggable.hashCode()
			return result
		}
	}

	object EmptySearchResult : SourceConfigItem
}