package org.koitharu.kotatsu.search.ui.suggestion.model

import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.areItemsEquals

sealed interface SearchSuggestionItem {

	class MangaList(
		val items: List<Manga>,
	) : SearchSuggestionItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as MangaList

			return items.areItemsEquals(other.items) { a, b ->
				a.title == b.title && a.coverUrl == b.coverUrl
			}
		}

		override fun hashCode(): Int {
			return items.fold(0) { acc, t ->
				var r = 31 * acc + t.title.hashCode()
				r = 31 * r + t.coverUrl.hashCode()
				r
			}
		}
	}

	class RecentQuery(
		val query: String,
	) : SearchSuggestionItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as RecentQuery

			if (query != other.query) return false

			return true
		}

		override fun hashCode(): Int {
			return query.hashCode()
		}
	}

	class Header(
		val source: MangaSource,
		val isChecked: MutableStateFlow<Boolean>,
	) : SearchSuggestionItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Header

			if (source != other.source) return false
			if (isChecked !== other.isChecked) return false

			return true
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + isChecked.hashCode()
			return result
		}
	}

	class Tags(
		val tags: List<ChipsView.ChipModel>,
	) : SearchSuggestionItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Tags

			if (tags != other.tags) return false

			return true
		}

		override fun hashCode(): Int {
			return tags.hashCode()
		}
	}
}
