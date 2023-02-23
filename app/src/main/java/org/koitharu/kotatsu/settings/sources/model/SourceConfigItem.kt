package org.koitharu.kotatsu.settings.sources.model

import androidx.annotation.DrawableRes
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
		val summary: String?,
		val isDraggable: Boolean,
	) : SourceConfigItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as SourceItem

			if (source != other.source) return false
			if (summary != other.summary) return false
			if (isEnabled != other.isEnabled) return false
			if (isDraggable != other.isDraggable) return false

			return true
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + summary.hashCode()
			result = 31 * result + isEnabled.hashCode()
			result = 31 * result + isDraggable.hashCode()
			return result
		}
	}

	class Tip(
		val key: String,
		@DrawableRes val iconResId: Int,
		@StringRes val textResId: Int,
	) : SourceConfigItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Tip

			if (key != other.key) return false
			if (iconResId != other.iconResId) return false
			if (textResId != other.textResId) return false

			return true
		}

		override fun hashCode(): Int {
			var result = key.hashCode()
			result = 31 * result + iconResId
			result = 31 * result + textResId
			return result
		}
	}

	object EmptySearchResult : SourceConfigItem
}
