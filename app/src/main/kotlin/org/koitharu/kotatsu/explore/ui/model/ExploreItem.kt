package org.koitharu.kotatsu.explore.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface ExploreItem : ListModel {

	class Buttons(
		val isSuggestionsEnabled: Boolean
	) : ExploreItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Buttons

			return isSuggestionsEnabled == other.isSuggestionsEnabled
		}

		override fun hashCode(): Int {
			return isSuggestionsEnabled.hashCode()
		}
	}

	class Header(
		@StringRes val titleResId: Int,
		val isButtonVisible: Boolean,
	) : ExploreItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Header

			if (titleResId != other.titleResId) return false
			return isButtonVisible == other.isButtonVisible
		}

		override fun hashCode(): Int {
			var result = titleResId
			result = 31 * result + isButtonVisible.hashCode()
			return result
		}
	}

	class Source(
		val source: MangaSource,
		val isGrid: Boolean,
	) : ExploreItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Source

			if (source != other.source) return false
			return isGrid == other.isGrid
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + isGrid.hashCode()
			return result
		}
	}

	class EmptyHint(
		@DrawableRes icon: Int,
		@StringRes textPrimary: Int,
		@StringRes textSecondary: Int,
		@StringRes actionStringRes: Int,
	) : EmptyState(icon, textPrimary, textSecondary, actionStringRes), ExploreItem

	object Loading : ExploreItem {

		override fun equals(other: Any?): Boolean = other === Loading
	}
}
