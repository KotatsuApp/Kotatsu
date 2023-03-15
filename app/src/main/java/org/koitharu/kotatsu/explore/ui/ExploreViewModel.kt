package org.koitharu.kotatsu.explore.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.ReversibleHandle
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.explore.domain.ExploreRepository
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.asFlowLiveData
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
	private val settings: AppSettings,
	private val exploreRepository: ExploreRepository,
) : BaseViewModel() {

	private val gridMode = settings.observeAsStateFlow(
		key = AppSettings.KEY_SOURCES_GRID,
		scope = viewModelScope + Dispatchers.IO,
		valueProducer = { isSourcesGridMode },
	)

	val onOpenManga = SingleLiveEvent<Manga>()
	val onActionDone = SingleLiveEvent<ReversibleAction>()
	val isGrid = gridMode.asFlowLiveData(viewModelScope.coroutineContext)

	val content: LiveData<List<ExploreItem>> = isLoading.asFlow().flatMapLatest { loading ->
		if (loading) {
			flowOf(listOf(ExploreItem.Loading))
		} else {
			createContentFlow()
		}
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, listOf(ExploreItem.Loading))

	fun openRandom() {
		launchLoadingJob(Dispatchers.Default) {
			val manga = exploreRepository.findRandomManga(tagsLimit = 8)
			onOpenManga.postCall(manga)
		}
	}

	fun hideSource(source: MangaSource) {
		launchJob(Dispatchers.Default) {
			settings.hiddenSources += source.name
			val rollback = ReversibleHandle {
				settings.hiddenSources -= source.name
			}
			onActionDone.postCall(ReversibleAction(R.string.source_disabled, rollback))
		}
	}

	fun setGridMode(value: Boolean) {
		settings.isSourcesGridMode = value
	}

	private fun createContentFlow() = settings.observe()
		.filter {
			it == AppSettings.KEY_SOURCES_HIDDEN ||
				it == AppSettings.KEY_SOURCES_ORDER ||
				it == AppSettings.KEY_SUGGESTIONS
		}
		.onStart { emit("") }
		.map { settings.getMangaSources(includeHidden = false) }
		.distinctUntilChanged()
		.combine(gridMode) { content, grid -> buildList(content, grid) }

	private fun buildList(sources: List<MangaSource>, isGrid: Boolean): List<ExploreItem> {
		val result = ArrayList<ExploreItem>(sources.size + 3)
		result += ExploreItem.Buttons(
			isSuggestionsEnabled = settings.isSuggestionsEnabled,
		)
		result += ExploreItem.Header(R.string.remote_sources, sources.isNotEmpty())
		if (sources.isNotEmpty()) {
			sources.mapTo(result) { ExploreItem.Source(it, isGrid) }
		} else {
			result += ExploreItem.EmptyHint(
				icon = R.drawable.ic_empty_common,
				textPrimary = R.string.no_manga_sources,
				textSecondary = R.string.no_manga_sources_text,
				actionStringRes = R.string.manage,
			)
		}
		return result
	}
}
