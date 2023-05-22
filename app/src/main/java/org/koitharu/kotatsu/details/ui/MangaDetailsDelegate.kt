package org.koitharu.kotatsu.details.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.util.RetainedLifecycleCoroutineScope
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.util.ext.printStackTraceDebug
import javax.inject.Inject

@ViewModelScoped
class MangaDetailsDelegate @Inject constructor(
	savedStateHandle: SavedStateHandle,
	lifecycle: ViewModelLifecycle,
	private val mangaDataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	networkState: NetworkState,
) {
	private val viewModelScope = RetainedLifecycleCoroutineScope(lifecycle)

	private val intent = MangaIntent(savedStateHandle)
	private val onlineMangaStateFlow = MutableStateFlow<Manga?>(null)
	private val localMangaStateFlow = MutableStateFlow<Manga?>(null)

	val onlineManga = combine(
		onlineMangaStateFlow,
		networkState,
	) { m, s -> m.takeIf { s } }
		.stateIn(viewModelScope, SharingStarted.Lazily, null)
	val localManga = localMangaStateFlow.asStateFlow()

	val selectedBranch = MutableStateFlow<String?>(null)
	val mangaId = intent.manga?.id ?: intent.mangaId

	init {
		intent.manga?.let {
			publishManga(it)
		}
	}

	suspend fun doLoad() {
		var manga = mangaDataRepository.resolveIntent(intent) ?: throw NotFoundException("Cannot find manga", "")
		publishManga(manga)
		manga = mangaRepositoryFactory.create(manga.source).getDetails(manga)
		// find default branch
		val hist = historyRepository.getOne(manga)
		selectedBranch.value = manga.getPreferredBranch(hist)
		publishManga(manga)
		runCatchingCancellable {
			if (manga.source == MangaSource.LOCAL) {
				val m = localMangaRepository.getRemoteManga(manga) ?: return@runCatchingCancellable null
				mangaRepositoryFactory.create(m.source).getDetails(m)
			} else {
				localMangaRepository.findSavedManga(manga)?.manga
			}
		}.onFailure { error ->
			error.printStackTraceDebug()
		}.onSuccess {
			if (it != null) {
				publishManga(it)
			}
		}
	}

	fun publishManga(manga: Manga) {
		if (manga.source == MangaSource.LOCAL) {
			localMangaStateFlow
		} else {
			onlineMangaStateFlow
		}.value = manga
	}
}
