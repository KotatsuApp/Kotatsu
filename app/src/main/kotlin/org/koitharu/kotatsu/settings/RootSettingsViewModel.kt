package org.koitharu.kotatsu.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import javax.inject.Inject

@HiltViewModel
class RootSettingsViewModel @Inject constructor(
	sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	val totalSourcesCount = sourcesRepository.allMangaSources.size

	val enabledSourcesCount = sourcesRepository.observeEnabledSourcesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, -1)
}
