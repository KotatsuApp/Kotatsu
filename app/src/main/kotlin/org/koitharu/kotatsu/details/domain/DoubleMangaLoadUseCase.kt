package org.koitharu.kotatsu.details.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.details.domain.model.DoubleManga
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

class DoubleMangaLoadUseCase @Inject constructor(
	private val mangaDataRepository: MangaDataRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga): DoubleManga = coroutineScope {
		val remoteDeferred = async(Dispatchers.Default) { loadRemote(manga) }
		val localDeferred = async(Dispatchers.Default) { loadLocal(manga) }
		DoubleManga(
			remoteManga = remoteDeferred.await(),
			localManga = localDeferred.await(),
		)
	}

	suspend operator fun invoke(mangaId: Long): DoubleManga {
		val manga = mangaDataRepository.findMangaById(mangaId) ?: throwNFE()
		return invoke(manga)
	}

	suspend operator fun invoke(intent: MangaIntent): DoubleManga {
		val manga = mangaDataRepository.resolveIntent(intent) ?: throwNFE()
		return invoke(manga)
	}

	private suspend fun loadLocal(manga: Manga): Result<Manga>? {
		return runCatchingCancellable {
			if (manga.isLocal) {
				localMangaRepository.getDetails(manga)
			} else {
				localMangaRepository.findSavedManga(manga)?.manga
			} ?: return null
		}
	}

	private suspend fun loadRemote(manga: Manga): Result<Manga>? {
		return runCatchingCancellable {
			val seed = if (manga.isLocal) {
				localMangaRepository.getRemoteManga(manga)
			} else {
				manga
			} ?: return null
			val repository = mangaRepositoryFactory.create(seed.source)
			repository.getDetails(seed)
		}
	}

	private fun throwNFE(): Nothing = throw NotFoundException("Cannot find manga", "")
}
