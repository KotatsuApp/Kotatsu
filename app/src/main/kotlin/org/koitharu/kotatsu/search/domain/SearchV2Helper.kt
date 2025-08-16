package org.koitharu.kotatsu.search.domain

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.contains
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.almostEquals
import org.koitharu.kotatsu.parsers.util.levenshteinDistance
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable

private const val MATCH_THRESHOLD_DEFAULT = 0.2f

class SearchV2Helper @AssistedInject constructor(
	@Assisted private val source: MangaSource,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val dataRepository: MangaDataRepository,
	private val settings: AppSettings,
) {

	suspend operator fun invoke(query: String, kind: SearchKind): SearchResults? {
		if (settings.isNsfwContentDisabled && source.isNsfw()) {
			return null
		}
		val repository = mangaRepositoryFactory.create(source)
		val listFilter = repository.getFilter(query, kind) ?: return null
		val sortOrder = repository.getSortOrder(kind)
		val list = repository.getList(0, sortOrder, listFilter)
		if (list.isEmpty()) {
			return null
		}
		val result = list.toMutableList()
		result.postFilter(query, kind)
		result.sortByRelevance(query, kind)
		return SearchResults(listFilter = listFilter, sortOrder = sortOrder, manga = result)
	}

	private suspend fun MangaRepository.getFilter(query: String, kind: SearchKind): MangaListFilter? = when (kind) {
		SearchKind.SIMPLE,
		SearchKind.TITLE -> if (filterCapabilities.isSearchSupported) {
			MangaListFilter(query = query)
		} else {
			null
		}

		SearchKind.AUTHOR -> if (filterCapabilities.isAuthorSearchSupported) {
			MangaListFilter(author = query)
		} else if (filterCapabilities.isSearchSupported) {
			MangaListFilter(query = query)
		} else {
			null
		}

		SearchKind.TAG -> {
			val tags = this@SearchV2Helper.dataRepository.findTags(this.source) + runCatchingCancellable {
				this@getFilter.getFilterOptions().availableTags
			}.onFailure { e ->
				e.printStackTraceDebug()
			}.getOrDefault(emptySet())
			val tag = tags.find { x -> x.title.equals(query, ignoreCase = true) }
			if (tag != null) {
				MangaListFilter(tags = setOf(tag))
			} else {
				null
			}
		}
	}

	private fun MutableList<Manga>.postFilter(query: String, kind: SearchKind) {
		if (settings.isNsfwContentDisabled) {
			removeAll { it.isNsfw() }
		}
		when (kind) {
			SearchKind.TITLE -> retainAll { m ->
				m.matches(query, MATCH_THRESHOLD_DEFAULT)
			}

			SearchKind.AUTHOR -> retainAll { m ->
				m.authors.isEmpty() || m.authors.contains(query, ignoreCase = true)
			}

			SearchKind.SIMPLE, // no filtering expected
			SearchKind.TAG -> Unit
		}
	}

	private fun MutableList<Manga>.sortByRelevance(query: String, kind: SearchKind) {
		when (kind) {
			SearchKind.SIMPLE,
			SearchKind.TITLE -> sortBy { m ->
				minOf(m.title.levenshteinDistance(query), m.altTitle?.levenshteinDistance(query) ?: Int.MAX_VALUE)
			}

			SearchKind.AUTHOR -> sortByDescending { m ->
				m.authors.contains(query, ignoreCase = true)
			}

			SearchKind.TAG -> sortByDescending { m ->
				m.tags.any { tag -> tag.title.equals(query, ignoreCase = true) }
			}
		}
	}

	private fun MangaRepository.getSortOrder(kind: SearchKind): SortOrder {
		val preferred: SortOrder = when (kind) {
			SearchKind.SIMPLE,
			SearchKind.TITLE,
			SearchKind.AUTHOR -> SortOrder.RELEVANCE

			SearchKind.TAG -> SortOrder.POPULARITY
		}
		return if (preferred in sortOrders) {
			preferred
		} else {
			defaultSortOrder
		}
	}


	private fun Manga.matches(query: String, threshold: Float): Boolean {
		return matchesTitles(title, query, threshold) || matchesTitles(altTitle, query, threshold)
	}

	private fun matchesTitles(a: String?, b: String?, threshold: Float): Boolean {
		return !a.isNullOrEmpty() && !b.isNullOrEmpty() && a.almostEquals(b, threshold)
	}

	@AssistedFactory
	interface Factory {

		fun create(source: MangaSource): SearchV2Helper
	}
}
