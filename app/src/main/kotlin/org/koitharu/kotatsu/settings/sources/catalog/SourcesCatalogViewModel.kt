package org.koitharu.kotatsu.settings.sources.catalog

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.internal.lifecycle.RetainedLifecycleImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.LocaleComparator
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.sortedWithSafe
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SourcesCatalogViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
	private val listProducerFactory: SourcesCatalogListProducer.Factory,
	settings: AppSettings,
) : BaseViewModel() {

	private val lifecycle = RetainedLifecycleImpl()
	private var searchQuery: String? = null
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val contentType = MutableStateFlow(ContentType.entries.first())
	val locales = getLocalesImpl()
	val locale = MutableStateFlow(locales.firstOrNull()?.language)

	val isNsfwDisabled = settings.isNsfwContentDisabled

	private val listProducer: StateFlow<SourcesCatalogListProducer?> = combine(
		locale,
		contentType,
	) { lc, type ->
		listProducerFactory.create(lc, type, lifecycle).also {
			it.setQuery(searchQuery)
		}
	}.stateIn(viewModelScope, SharingStarted.Eagerly, null)

	val content = listProducer.flatMapLatest {
		it?.list ?: emptyFlow()
	}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

	override fun onCleared() {
		super.onCleared()
		lifecycle.dispatchOnCleared()
	}

	fun performSearch(query: String?) {
		searchQuery = query
		listProducer.value?.setQuery(query)
	}

	fun setLocale(value: String?) {
		locale.value = value
	}

	fun setContentType(value: ContentType) {
		contentType.value = value
	}

	fun addSource(source: MangaSource) {
		launchJob(Dispatchers.Default) {
			val rollback = repository.setSourceEnabled(source, true)
			onActionDone.call(ReversibleAction(R.string.source_enabled, rollback))
		}
	}

	private fun getLocalesImpl(): List<Locale?> {
		return repository.allMangaSources
			.mapToSet { it.locale?.let(::Locale) }
			.sortedWithSafe(LocaleComparator())
	}
}
