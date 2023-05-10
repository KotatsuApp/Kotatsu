package org.koitharu.kotatsu.explore.domain

import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.suggestions.domain.TagsBlacklist
import javax.inject.Inject

class ExploreRepository @Inject constructor(
	private val settings: AppSettings,
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend fun findRandomManga(tagsLimit: Int): Manga {
		val blacklistTagRegex = TagsBlacklist(settings.suggestionsTagsBlacklist, 0.4f)
		val allTags = historyRepository.getPopularTags(tagsLimit).filterNot {
			it in blacklistTagRegex
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
			if (item in blacklistTagRegex) {
				continue
			}
			return item
		}
		return list.random()
	}
}
