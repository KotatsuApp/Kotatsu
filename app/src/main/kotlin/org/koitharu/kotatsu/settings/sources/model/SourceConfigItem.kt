package org.koitharu.kotatsu.settings.sources.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface SourceConfigItem : ListModel {

	data class Header(
		@StringRes val titleResId: Int,
	) : SourceConfigItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Header && other.titleResId == titleResId
		}
	}

	data class LocaleGroup(
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
	}

	data class SourceItem(
		val source: MangaSource,
		val isEnabled: Boolean,
		val summary: String?,
		val isDraggable: Boolean,
		val isAvailable: Boolean,
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
	}

	data class Tip(
		val key: String,
		@DrawableRes val iconResId: Int,
		@StringRes val textResId: Int,
	) : SourceConfigItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tip && other.key == key
		}
	}

	data object EmptySearchResult : SourceConfigItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is EmptySearchResult
		}
	}
}
