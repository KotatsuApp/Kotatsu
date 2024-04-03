package org.koitharu.kotatsu.details.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.scrobbling.common.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import javax.inject.Inject

/* TODO: remove */
class DetailsInteractor @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
) {

	fun observeIsFavourite(mangaId: Long): Flow<Boolean> {
		return favouritesRepository.observeCategoriesIds(mangaId)
			.map { it.isNotEmpty() }
	}

	fun observeFavourite(mangaId: Long): Flow<Set<FavouriteCategory>> {
		return favouritesRepository.observeCategories(mangaId)
	}

	fun observeNewChapters(mangaId: Long): Flow<Int> {
		return settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled }
			.flatMapLatest { isEnabled ->
				if (isEnabled) {
					trackingRepository.observeNewChaptersCount(mangaId)
				} else {
					flowOf(0)
				}
			}
	}

	fun observeScrobblingInfo(mangaId: Long): Flow<List<ScrobblingInfo>> {
		return combine(
			scrobblers.map { it.observeScrobblingInfo(mangaId) },
		) { scrobblingInfo ->
			scrobblingInfo.filterNotNull()
		}
	}

	fun observeIncognitoMode(mangaFlow: Flow<Manga?>): Flow<Boolean> {
		return mangaFlow
			.distinctUntilChangedBy { it?.isNsfw }
			.flatMapLatest { manga ->
				if (manga != null) {
					historyRepository.observeShouldSkip(manga)
				} else {
					settings.observeAsFlow(AppSettings.KEY_INCOGNITO_MODE) { isIncognitoModeEnabled }
				}
			}
	}

	suspend fun updateLocal(subject: MangaDetails?, localManga: LocalManga): MangaDetails? {
		subject ?: return null
		return if (subject.id == localManga.manga.id) {
			if (subject.isLocal) {
				subject.copy(
					manga = localManga.manga,
				)
			} else {
				subject.copy(
					localManga = runCatchingCancellable {
						localManga.copy(
							manga = localMangaRepository.getDetails(localManga.manga),
						)
					}.getOrNull() ?: subject.local,
				)
			}
		} else {
			subject
		}
	}

	suspend fun findRemote(seed: Manga) = localMangaRepository.getRemoteManga(seed)
}
