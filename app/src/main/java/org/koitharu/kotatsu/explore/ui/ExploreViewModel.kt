package org.koitharu.kotatsu.explore.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.explore.domain.ExploreRepository
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

@HiltViewModel
class ExploreViewModel @Inject constructor(
	private val settings: AppSettings,
	private val exploreRepository: ExploreRepository,
) : BaseViewModel() {

	val onOpenManga = SingleLiveEvent<Manga>()

	val content: LiveData<List<ExploreItem>> = isLoading.asFlow().flatMapLatest { loading ->
		if (loading) {
			flowOf(listOf(ExploreItem.Loading))
		} else {
			createContentFlow()
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(ExploreItem.Loading))

	fun openRandom() {
		launchLoadingJob(Dispatchers.Default) {
			val manga = exploreRepository.findRandomManga(tagsLimit = 8)
			onOpenManga.postCall(manga)
		}
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
		.map { buildList(it) }

	private fun buildList(sources: List<MangaSource>): List<ExploreItem> {
		val result = ArrayList<ExploreItem>(sources.size + 3)
		result += ExploreItem.Buttons(
			isSuggestionsEnabled = settings.isSuggestionsEnabled,
		)
		result += ExploreItem.Header(R.string.remote_sources, sources.isNotEmpty())
		if (sources.isNotEmpty()) {
			sources.mapTo(result) { ExploreItem.Source(it) }
		} else {
			result += ExploreItem.EmptyHint(
				icon = R.drawable.ic_empty_search,
				textPrimary = R.string.no_manga_sources,
				textSecondary = R.string.no_manga_sources_text,
				actionStringRes = R.string.manage,
			)
		}
		return result
	}
}
