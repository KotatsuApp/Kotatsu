package org.koitharu.kotatsu.settings.sources.catalog

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaSource

sealed interface SourceCatalogItem : ListModel {

	data class Source(
		val source: MangaSource,
		val showSummary: Boolean,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source == source
		}
	}

	data class Hint(
		@DrawableRes val icon: Int,
		@StringRes val title: Int,
		@StringRes val text: Int,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Hint && other.title == title
		}
	}
}
