package org.koitharu.kotatsu.stats.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.stats.data.StatsRepository
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
	private val repository: StatsRepository,
) : BaseViewModel() {

	val readingStats = flow {
		emit(repository.getReadingStats())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())
}
