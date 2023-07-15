package org.koitharu.kotatsu.explore.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.ui.util.ReversibleHandle
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.explore.domain.ExploreRepository
import org.koitharu.kotatsu.explore.ui.model.ExploreButtons
import org.koitharu.kotatsu.explore.ui.model.MangaSourceItem
import org.koitharu.kotatsu.explore.ui.model.RecommendationsItem
import org.koitharu.kotatsu.list.ui.model.EmptyHint
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.TipModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
	private val settings: AppSettings,
	private val suggestionRepository: SuggestionRepository,
	private val exploreRepository: ExploreRepository,
) : BaseViewModel() {

	val isGrid = settings.observeAsStateFlow(
		key = AppSettings.KEY_SOURCES_GRID,
		scope = viewModelScope + Dispatchers.IO,
		valueProducer = { isSourcesGridMode },
	)

	val onOpenManga = MutableEventFlow<Manga>()
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val onShowSuggestionsTip = MutableEventFlow<Unit>()
	private val isRandomLoading = MutableStateFlow(false)
	private val recommendationDeferred = viewModelScope.async(Dispatchers.Default) {
		runCatchingCancellable {
			suggestionRepository.getRandom()
		}.getOrNull()
	}

	val content: StateFlow<List<ListModel>> = isLoading.flatMapLatest { loading ->
		if (loading) {
			flowOf(getLoadingStateList())
		} else {
			createContentFlow()
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, getLoadingStateList())

	init {
		launchJob(Dispatchers.Default) {
			if (!settings.isSuggestionsEnabled && settings.isTipEnabled(TIP_SUGGESTIONS)) {
				onShowSuggestionsTip.call(Unit)
			}
		}
	}

	fun openRandom() {
		if (isRandomLoading.value) {
			return
		}
		launchJob(Dispatchers.Default) {
			isRandomLoading.value = true
			try {
				val manga = exploreRepository.findRandomManga(tagsLimit = 8)
				onOpenManga.call(manga)
			} finally {
				isRandomLoading.value = false
			}
		}
	}

	fun hideSource(source: MangaSource) {
		launchJob(Dispatchers.Default) {
			settings.hiddenSources += source.name
			val rollback = ReversibleHandle {
				settings.hiddenSources -= source.name
			}
			onActionDone.call(ReversibleAction(R.string.source_disabled, rollback))
		}
	}

	fun setGridMode(value: Boolean) {
		settings.isSourcesGridMode = value
	}

	fun respondSuggestionTip(isAccepted: Boolean) {
		settings.isSuggestionsEnabled = isAccepted
		settings.closeTip(TIP_SUGGESTIONS)
	}

	private fun createContentFlow() = combine(
		observeSources(),
		isGrid,
		isRandomLoading,
		observeNewSources(),
	) { content, grid, randomLoading, newSources ->
		val recommendation = recommendationDeferred.await()
		buildList(content, recommendation, grid, randomLoading, newSources)
	}

	private fun buildList(
		sources: List<MangaSource>,
		recommendation: Manga?,
		isGrid: Boolean,
		randomLoading: Boolean,
		newSources: Set<MangaSource>,
	): List<ListModel> {
		val result = ArrayList<ListModel>(sources.size + 4)
		result += ExploreButtons(randomLoading)
		if (recommendation != null) {
			result += ListHeader(R.string.suggestions, 0, null)
			result += RecommendationsItem(recommendation)
		}
		if (sources.isNotEmpty()) {
			result += ListHeader(R.string.remote_sources, R.string.manage, null)
			if (newSources.isNotEmpty()) {
				result += TipModel(
					key = TIP_NEW_SOURCES,
					title = R.string.new_sources_text,
					text = R.string.new_sources_text,
					icon = R.drawable.ic_explore_normal,
					primaryButtonText = R.string.manage,
					secondaryButtonText = R.string.discard,
				)
			}
			sources.mapTo(result) { MangaSourceItem(it, isGrid) }
		} else {
			result += EmptyHint(
				icon = R.drawable.ic_empty_common,
				textPrimary = R.string.no_manga_sources,
				textSecondary = R.string.no_manga_sources_text,
				actionStringRes = R.string.manage,
			)
		}
		return result
	}

	private fun observeSources() = settings.observe()
		.filter {
			it == AppSettings.KEY_SOURCES_HIDDEN ||
				it == AppSettings.KEY_SOURCES_ORDER ||
				it == AppSettings.KEY_SUGGESTIONS
		}
		.onStart { emit("") }
		.map { settings.getMangaSources(includeHidden = false) }

	private fun getLoadingStateList() = listOf(
		ExploreButtons(isRandomLoading.value),
		LoadingState,
	)

	private fun observeNewSources() = settings.observe()
		.filter { it == AppSettings.KEY_SOURCES_ORDER || it == AppSettings.KEY_SOURCES_HIDDEN }
		.onStart { emit("") }
		.map { settings.newSources }
		.distinctUntilChanged()

	companion object {

		private const val TIP_SUGGESTIONS = "suggestions"
		const val TIP_NEW_SOURCES = "new_sources"
	}
}
