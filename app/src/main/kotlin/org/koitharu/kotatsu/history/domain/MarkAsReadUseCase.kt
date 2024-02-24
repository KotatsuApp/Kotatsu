package org.koitharu.kotatsu.history.domain

import dagger.Reusable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@Reusable
class MarkAsReadUseCase @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga) {
		val repo = mangaRepositoryFactory.create(manga.source)
		val details = if (manga.chapters.isNullOrEmpty()) {
			repo.getDetails(manga)
		} else {
			manga
		}
		val lastChapter = checkNotNull(details.chapters).last()
		val pages = repo.getPages(lastChapter)
		historyRepository.addOrUpdate(
			manga = details,
			chapterId = lastChapter.id,
			page = pages.lastIndex,
			scroll = 0,
			percent = 1f,
			force = true,
		)
	}

	suspend operator fun invoke(manga: Collection<Manga>) {
		when (manga.size) {
			0 -> Unit
			1 -> invoke(manga.first())
			else -> supervisorScope {
				manga.map {
					launch {
						invoke(it)
					}
				}.joinAll()
			}
		}
	}
}
