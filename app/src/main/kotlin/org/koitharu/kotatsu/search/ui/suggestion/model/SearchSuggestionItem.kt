package org.koitharu.kotatsu.search.ui.suggestion.model

import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.areItemsEquals

sealed interface SearchSuggestionItem : ListModel {

	class MangaList(
		val items: List<Manga>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is MangaList
		}

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

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is RecentQuery && query == other.query
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as RecentQuery

			return query == other.query
		}

		override fun hashCode(): Int {
			return query.hashCode()
		}
	}

	class Source(
		val source: MangaSource,
		val isEnabled: Boolean,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source == source
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			if (previousState !is Source) {
				return super.getChangePayload(previousState)
			}
			return if (isEnabled != previousState.isEnabled) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				null
			}
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Source

			if (source != other.source) return false
			return isEnabled == other.isEnabled
		}

		override fun hashCode(): Int {
			var result = source.hashCode()
			result = 31 * result + isEnabled.hashCode()
			return result
		}
	}

	class Tags(
		val tags: List<ChipsView.ChipModel>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tags
		}

		override fun getChangePayload(previousState: ListModel): Any {
			return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Tags

			return tags == other.tags
		}

		override fun hashCode(): Int {
			return tags.hashCode()
		}
	}
}
