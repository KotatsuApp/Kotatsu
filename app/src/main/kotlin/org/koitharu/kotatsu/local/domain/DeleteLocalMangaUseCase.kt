package org.koitharu.kotatsu.local.domain

import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.IOException
import javax.inject.Inject

class DeleteLocalMangaUseCase @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
) {

	suspend operator fun invoke(manga: Manga) {
		val victim = if (manga.isLocal) manga else localMangaRepository.findSavedManga(manga)?.manga
		checkNotNull(victim) { "Cannot find saved manga for ${manga.title}" }
		val original = if (manga.isLocal) localMangaRepository.getRemoteManga(manga) else manga
		localMangaRepository.delete(victim) || throw IOException("Unable to delete file")
		runCatchingCancellable {
			historyRepository.deleteOrSwap(victim, original)
		}.onFailure {
			it.printStackTraceDebug()
		}
	}

	suspend operator fun invoke(ids: Set<Long>) {
		val list = localMangaRepository.getList(0, null)
		var removed = 0
		for (manga in list) {
			if (manga.id in ids) {
				invoke(manga)
				removed++
			}
		}
		check(removed == ids.size) {
			"Removed $removed files but ${ids.size} requested"
		}
	}
}
