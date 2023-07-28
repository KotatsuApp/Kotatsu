package org.koitharu.kotatsu.settings.sources.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface SourceConfigItem : ListModel {

	class Header(
		@StringRes val titleResId: Int,
	) : SourceConfigItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Header && other.titleResId == titleResId
		}

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

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is LocaleGroup && other.localeId == localeId
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			return if (previousState is LocaleGroup && previousState.isExpanded != isExpanded) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				super.getChangePayload(previousState)
			}
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as LocaleGroup

			if (localeId != other.localeId) return false
			if (title != other.title) return false
			return isExpanded == other.isExpanded
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

		val isNsfw: Boolean
			get() = source.contentType == ContentType.HENTAI

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is SourceItem && other.source == source
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			return if (previousState is SourceItem && previousState.isEnabled != isEnabled) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				super.getChangePayload(previousState)
			}
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as SourceItem

			if (source != other.source) return false
			if (summary != other.summary) return false
			if (isEnabled != other.isEnabled) return false
			return isDraggable == other.isDraggable
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

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tip && other.key == key
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Tip

			if (key != other.key) return false
			if (iconResId != other.iconResId) return false
			return textResId == other.textResId
		}

		override fun hashCode(): Int {
			var result = key.hashCode()
			result = 31 * result + iconResId
			result = 31 * result + textResId
			return result
		}
	}

	object EmptySearchResult : SourceConfigItem {

		override fun equals(other: Any?): Boolean {
			return other === EmptySearchResult
		}

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is EmptySearchResult
		}
	}
}
