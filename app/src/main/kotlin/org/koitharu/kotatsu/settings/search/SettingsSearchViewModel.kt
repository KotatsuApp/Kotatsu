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

	private val query = MutableStateFlow("")
	private val allSettings by lazy {
		searchHelper.inflatePreferences()
	}

	val content = query.map { q ->
		allSettings.filter { it.title.contains(q, ignoreCase = true) }
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	val isSearchActive = query.map {
		it.isNotEmpty()
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val onNavigateToPreference = MutableEventFlow<SettingsItem>()
	val currentQuery: String
		get() = query.value

	fun onQueryChanged(value: String) {
		query.value = value
	}

	fun discardSearch() = onQueryChanged("")

	fun navigateToPreference(item: SettingsItem) {
		onNavigateToPreference.call(item)
	}
}
