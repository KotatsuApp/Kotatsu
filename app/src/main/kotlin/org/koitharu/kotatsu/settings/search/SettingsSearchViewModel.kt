package org.koitharu.kotatsu.settings.search

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import javax.inject.Inject

@HiltViewModel
class SettingsSearchViewModel @Inject constructor(
	private val searchHelper: SettingsSearchHelper,
) : BaseViewModel() {

	private val query = MutableStateFlow<String?>(null)
	private val allSettings by lazy {
		searchHelper.inflatePreferences()
	}

	val content = query.map { q ->
		if (q == null) {
			emptyList()
		} else {
			allSettings.filter { it.title.contains(q, ignoreCase = true) }
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	val isSearchActive = query.map {
		it != null
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, query.value != null)

	val onNavigateToPreference = MutableEventFlow<SettingsItem>()
	val currentQuery: String
		get() = query.value.orEmpty()

	fun onQueryChanged(value: String) {
		if (query.value != null) {
			query.value = value
		}
	}

	fun discardSearch() {
		query.value = null
	}

	fun startSearch() {
		query.value = query.value.orEmpty()
	}

	fun navigateToPreference(item: SettingsItem) {
		discardSearch()
		onNavigateToPreference.call(item)
	}
}
