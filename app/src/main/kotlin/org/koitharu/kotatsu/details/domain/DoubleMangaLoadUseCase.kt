package org.koitharu.kotatsu.details.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.details.domain.model.DoubleManga
import org.koitharu.kotatsu.explore.domain.RecoverMangaUseCase
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.recoverNotNull
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

class DoubleMangaLoadUseCase @Inject constructor(
	private val mangaDataRepository: MangaDataRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val recoverUseCase: RecoverMangaUseCase,
) {

	operator fun invoke(manga: Manga): Flow<DoubleManga> = flow<DoubleManga> {
		var lastValue: DoubleManga? = null
		var emitted = false
		invokeImpl(manga).collect {
			lastValue = it
			if (it.any != null) {
				emitted = true
				emit(it)
			}
		}
		if (!emitted) {
			lastValue?.requireAny()
		}
	}.flowOn(Dispatchers.Default)

	operator fun invoke(mangaId: Long): Flow<DoubleManga> = flow {
		emit(mangaDataRepository.findMangaById(mangaId) ?: throwNFE())
	}.flatMapLatest { invoke(it) }

	operator fun invoke(intent: MangaIntent): Flow<DoubleManga> = flow {
		emit(mangaDataRepository.resolveIntent(intent) ?: throwNFE())
	}.flatMapLatest { invoke(it) }

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
		}.recoverNotNull { e ->
			if (e is NotFoundException) {
				recoverUseCase(manga)
			} else {
				null
			}
		}
	}

	private fun invokeImpl(manga: Manga): Flow<DoubleManga> = combine(
		flow { emit(null); emit(loadRemote(manga)) },
		flow { emit(null); emit(loadLocal(manga)) },
	) { remote, local ->
		DoubleManga(
			remoteManga = remote,
			localManga = local,
		)
	}

	private fun throwNFE(): Nothing = throw NotFoundException("Cannot find manga", "")
}
