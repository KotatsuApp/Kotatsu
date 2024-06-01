package org.koitharu.kotatsu.settings.sources.catalog

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.explore.data.SourcesSortOrder
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SourcesCatalogViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val locales = repository.allMangaSources.mapToSet { it.locale }

	private val searchQuery = MutableStateFlow<String?>(null)
	val appliedFilter = MutableStateFlow(
		SourcesCatalogFilter(
			types = emptySet(),
			locale = Locale.getDefault().language.takeIf { it in locales },
			isNewOnly = false,
		),
	)

	val isNsfwDisabled = settings.isNsfwContentDisabled

	val hasNewSources = repository.observeHasNewSources()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val content: StateFlow<List<SourceCatalogItem>> = combine(
		searchQuery,
		appliedFilter,
	) { q, f ->
		buildSourcesList(f, q)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	init {
		repository.clearNewSourcesBadge()
	}

	fun performSearch(query: String?) {
		searchQuery.value = query?.trim()
	}

	fun setLocale(value: String?) {
		appliedFilter.value = appliedFilter.value.copy(locale = value)
	}

	fun addSource(source: MangaSource) {
		launchJob(Dispatchers.Default) {
			val rollback = repository.setSourcesEnabled(setOf(source), true)
			onActionDone.call(ReversibleAction(R.string.source_enabled, rollback))
		}
	}

	fun setContentType(value: ContentType, isAdd: Boolean) {
		val filter = appliedFilter.value
		val types = EnumSet.noneOf(ContentType::class.java)
		types.addAll(filter.types)
		if (isAdd) {
			types.add(value)
		} else {
			types.remove(value)
		}
		appliedFilter.value = filter.copy(types = types)
	}

	fun setNewOnly(value: Boolean) {
		appliedFilter.value = appliedFilter.value.copy(isNewOnly = value)
	}

	private suspend fun buildSourcesList(filter: SourcesCatalogFilter, query: String?): List<SourceCatalogItem> {
		val sources = repository.getAvailableSources(
			isDisabledOnly = true,
			isNewOnly = filter.isNewOnly,
			excludeBroken = false,
			types = filter.types,
			query = query,
			sortOrder = SourcesSortOrder.ALPHABETIC,
		).filter { it.locale == filter.locale }
		return if (sources.isEmpty()) {
			listOf(
				if (query == null) {
					SourceCatalogItem.Hint(
						icon = R.drawable.ic_empty_feed,
						title = R.string.no_manga_sources,
						text = R.string.no_manga_sources_catalog_text,
					)
				} else {
					SourceCatalogItem.Hint(
						icon = R.drawable.ic_empty_feed,
						title = R.string.nothing_found,
						text = R.string.no_manga_sources_found,
					)
				},
			)
		} else {
			sources.map {
				SourceCatalogItem.Source(source = it)
			}
		}
	}
}
