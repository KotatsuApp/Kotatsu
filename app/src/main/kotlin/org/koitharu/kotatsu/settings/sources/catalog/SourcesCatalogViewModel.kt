package org.koitharu.kotatsu.settings.sources.catalog

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.ui.widgets.ChipsView.ChipModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SourcesCatalogViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val locales = repository.allMangaSources.mapToSet { it.locale }

	private val searchQuery = MutableStateFlow<String?>(null)
	val appliedFilter = MutableStateFlow(
		SourcesCatalogFilter(
			types = emptySet(),
			locale = Locale.getDefault().language.takeIf { it in locales },
		),
	)

	val hasNewSources = repository.observeNewSources()
		.map { it.isNotEmpty() }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val filter: StateFlow<List<ChipModel>> = appliedFilter.map {
		buildFilter(it)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, buildFilter(appliedFilter.value))

	val content: StateFlow<List<SourceCatalogItem>> = combine(
		searchQuery,
		appliedFilter,
	) { q, f ->
		buildSourcesList(f, q)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

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

	fun skipNewSources() {
		launchJob {
			repository.assimilateNewSources()
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

	private fun buildFilter(applied: SourcesCatalogFilter): List<ChipModel> = buildList(ContentType.entries.size) {
		for (ct in ContentType.entries) {
			add(
				ChipModel(
					tint = 0,
					title = ct.name,
					icon = 0,
					isCheckable = true,
					isChecked = ct in applied.types,
					data = ct,
				),
			)
		}
	}

	private suspend fun buildSourcesList(filter: SourcesCatalogFilter, query: String?): List<SourceCatalogItem> {
		val sources = repository.getDisabledSources().toMutableList()
		sources.retainAll {
			(filter.types.isEmpty() || it.contentType in filter.types) && it.locale == filter.locale
		}
		if (!query.isNullOrEmpty()) {
			sources.retainAll { it.title.contains(query, ignoreCase = true) }
		}
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
			sources.sortBy { it.title }
			sources.map {
				SourceCatalogItem.Source(
					source = it,
					showSummary = query != null,
				)
			}
		}
	}
}
