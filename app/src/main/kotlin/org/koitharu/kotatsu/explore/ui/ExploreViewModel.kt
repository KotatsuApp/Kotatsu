package org.koitharu.kotatsu.explore.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import javax.inject.Inject

private const val TIP_SUGGESTIONS = "suggestions"

@HiltViewModel
class ExploreViewModel @Inject constructor(
	private val settings: AppSettings,
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

	val content: StateFlow<List<ExploreItem>> = isLoading.flatMapLatest { loading ->
		if (loading) {
			flowOf(listOf(ExploreItem.Loading))
		} else {
			createContentFlow()
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(ExploreItem.Loading))

	init {
		launchJob(Dispatchers.Default) {
			if (!settings.isSuggestionsEnabled && settings.isTipEnabled(TIP_SUGGESTIONS)) {
				onShowSuggestionsTip.call(Unit)
			}
		}
	}

	fun openRandom() {
		launchLoadingJob(Dispatchers.Default) {
			val manga = exploreRepository.findRandomManga(tagsLimit = 8)
			onOpenManga.call(manga)
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

	private fun createContentFlow() = settings.observe()
		.filter {
			it == AppSettings.KEY_SOURCES_HIDDEN ||
				it == AppSettings.KEY_SOURCES_ORDER ||
				it == AppSettings.KEY_SUGGESTIONS
		}
		.onStart { emit("") }
		.map { settings.getMangaSources(includeHidden = false) }
		.combine(isGrid) { content, grid -> buildList(content, grid) }

	private fun buildList(sources: List<MangaSource>, isGrid: Boolean): List<ExploreItem> {
		val result = ArrayList<ExploreItem>(sources.size + 3)
		result += ExploreItem.Buttons(
			isSuggestionsEnabled = settings.isSuggestionsEnabled,
		)
		result += ExploreItem.Header(R.string.suggestions, isButtonVisible = false)
		result += ExploreItem.Recommendation(
			Manga(
				0,
				"Test",
				"Test",
				"Test",
				"Test",
				0f,
				false,
				"http://images.ctfassets.net/yadj1kx9rmg0/wtrHxeu3zEoEce2MokCSi/cf6f68efdcf625fdc060607df0f3baef/quwowooybuqbl6ntboz3.jpg",
				emptySet(),
				MangaState.ONGOING,
				"Test",
				"http://images.ctfassets.net/yadj1kx9rmg0/wtrHxeu3zEoEce2MokCSi/cf6f68efdcf625fdc060607df0f3baef/quwowooybuqbl6ntboz3.jpg",
				"Test",
				emptyList(),
				MangaSource.DESUME,
			),
		) // TODO
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
