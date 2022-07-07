package org.koitharu.kotatsu.explore.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class ExploreViewModel(
	private val settings: AppSettings,
) : BaseViewModel() {

	val content: LiveData<List<ExploreItem>> = settings.observe()
		.filter {
			it == AppSettings.KEY_SOURCES_HIDDEN ||
				it == AppSettings.KEY_SOURCES_ORDER ||
				it == AppSettings.KEY_SUGGESTIONS
		}
		.onStart { emit("") }
		.map { settings.getMangaSources(includeHidden = false) }
		.distinctUntilChanged()
		.map { buildList(it) }
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

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