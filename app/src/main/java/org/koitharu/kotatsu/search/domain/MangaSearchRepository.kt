package org.koitharu.kotatsu.search.domain

import android.annotation.SuppressLint
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.base.domain.MangaProviderFactory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.ext.levenshteinDistance

class MangaSearchRepository(private val settings: AppSettings) {

	fun globalSearch(query: String, concurrency: Int = DEFAULT_CONCURRENCY): Flow<Manga> =
		MangaProviderFactory.getSources(settings, includeHidden = false).asFlow()
			.flatMapMerge(concurrency) { source ->
				runCatching {
					source.repository.getList(0, query, SortOrder.POPULARITY)
				}.getOrElse {
					emptyList()
				}.asFlow()
			}.filter {
				match(it, query)
			}

	private companion object {

		private val REGEX_SPACE = Regex("\\s+")

		@SuppressLint("DefaultLocale")
		fun match(manga: Manga, query: String): Boolean {
			val words = HashSet<String>()
			words += manga.title.toLowerCase().split(REGEX_SPACE)
			words += manga.altTitle?.toLowerCase()?.split(REGEX_SPACE).orEmpty()
			val words2 = query.toLowerCase().split(REGEX_SPACE).toSet()
			for (w in words) {
				for (w2 in words2) {
					val diff = w.levenshteinDistance(w2) / ((w.length + w2.length) / 2f)
					if (diff < 0.5) {
						return true
					}
				}
			}
			return false
		}
	}
}