package org.koitharu.kotatsu.explore.domain

import javax.inject.Inject
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder

class ExploreRepository @Inject constructor(
	private val settings: AppSettings,
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend fun findRandomManga(tagsLimit: Int): Manga {
		val blacklistTagRegex = settings.getSuggestionsTagsBlacklistRegex()
		val allTags = historyRepository.getPopularTags(tagsLimit).filterNot {
			blacklistTagRegex?.containsMatchIn(it.title) ?: false
		}
		val tag = allTags.randomOrNull()
		val source = checkNotNull(tag?.source ?: settings.getMangaSources(includeHidden = false).randomOrNull()) {
			"No sources found"
		}
		val repo = mangaRepositoryFactory.create(source)
		val list = repo.getList(
			offset = 0,
			sortOrder = if (SortOrder.UPDATED in repo.sortOrders) SortOrder.UPDATED else null,
			tags = setOfNotNull(tag),
		).shuffled()
		for (item in list) {
			if (settings.isSuggestionsExcludeNsfw && item.isNsfw) {
				continue
			}
			if (blacklistTagRegex != null && item.tags.any { x -> blacklistTagRegex.containsMatchIn(x.title) }) {
				continue
			}
			return item
		}
		return list.random()
	}
}
