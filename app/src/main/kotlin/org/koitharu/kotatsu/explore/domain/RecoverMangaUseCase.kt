package org.koitharu.kotatsu.explore.domain

import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

class RecoverMangaUseCase @Inject constructor(
	private val mangaDataRepository: MangaDataRepository,
	private val repositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga): Manga? = runCatchingCancellable {
		if (manga.isLocal) {
			return@runCatchingCancellable null
		}
		val repository = repositoryFactory.create(manga.source)
		val list = repository.getList(offset = 0, null, MangaListFilter(query = manga.title))
		val newManga = list.find { x -> x.title == manga.title }?.let {
			repository.getDetails(it)
		} ?: return@runCatchingCancellable null
		val merged = merge(manga, newManga)
		mangaDataRepository.storeManga(merged, replaceExisting = true)
		merged
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrNull()

	private fun merge(
		broken: Manga,
		current: Manga,
	) = Manga(
		id = broken.id,
		title = current.title,
		altTitles = current.altTitles,
		url = current.url,
		publicUrl = current.publicUrl,
		rating = current.rating,
		contentRating = current.contentRating,
		coverUrl = current.coverUrl,
		tags = current.tags,
		state = current.state,
		authors = current.authors,
		largeCoverUrl = current.largeCoverUrl,
		description = current.description,
		chapters = current.chapters,
		source = current.source,
	)
}
