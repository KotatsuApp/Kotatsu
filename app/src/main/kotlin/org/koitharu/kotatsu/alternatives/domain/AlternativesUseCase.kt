package org.koitharu.kotatsu.alternatives.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.util.ext.almostEquals
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

private const val MAX_PARALLELISM = 4
private const val MATCH_THRESHOLD = 0.2f

class AlternativesUseCase @Inject constructor(
	private val sourcesRepository: MangaSourcesRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga): Flow<Manga> {
		val sources = getSources(manga.source)
		if (sources.isEmpty()) {
			return emptyFlow()
		}
		val semaphore = Semaphore(MAX_PARALLELISM)
		return channelFlow {
			for (source in sources) {
				val repository = mangaRepositoryFactory.create(source)
				if (!repository.isSearchSupported) {
					continue
				}
				launch {
					val list = runCatchingCancellable {
						semaphore.withPermit {
							repository.getList(offset = 0, filter = MangaListFilter.Search(manga.title))
						}
					}.getOrDefault(emptyList())
					for (item in list) {
						if (item.matches(manga)) {
							send(item)
						}
					}
				}
			}
		}.map {
			runCatchingCancellable {
				mangaRepositoryFactory.create(it.source).getDetails(it)
			}.getOrDefault(it)
		}
	}

	private suspend fun getSources(ref: MangaSource): List<MangaSource> {
		val result = ArrayList<MangaSource>(MangaParserSource.entries.size - 2)
		result.addAll(sourcesRepository.getEnabledSources())
		result.sortByDescending { it.priority(ref) }
		result.addAll(sourcesRepository.getDisabledSources().sortedByDescending { it.priority(ref) })
		return result
	}

	private fun Manga.matches(ref: Manga): Boolean {
		return matchesTitles(title, ref.title) ||
			matchesTitles(title, ref.altTitle) ||
			matchesTitles(altTitle, ref.title) ||
			matchesTitles(altTitle, ref.altTitle)

	}

	private fun matchesTitles(a: String?, b: String?): Boolean {
		return !a.isNullOrEmpty() && !b.isNullOrEmpty() && a.almostEquals(b, MATCH_THRESHOLD)
	}

	private fun MangaSource.priority(ref: MangaSource): Int {
		var res = 0
		if (this is MangaParserSource && ref is MangaParserSource) {
			if (locale == ref.locale) res += 2
			if (contentType == ref.contentType) res++
		}
		return res
	}
}
