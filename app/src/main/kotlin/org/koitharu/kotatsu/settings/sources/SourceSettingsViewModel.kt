package org.koitharu.kotatsu.settings.sources

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject

@HiltViewModel
class SourceSettingsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
) : BaseViewModel() {

	val source = savedStateHandle.require<MangaSource>(SourceSettingsFragment.EXTRA_SOURCE)
	val repository = mangaRepositoryFactory.create(source) as RemoteMangaRepository

	val username = MutableStateFlow<String?>(null)
	private var usernameLoadJob: Job? = null

	init {
		loadUsername()
	}

	fun onResume() {
		if (usernameLoadJob?.isActive != true) {
			loadUsername()
		}
	}

	private fun loadUsername() {
		launchLoadingJob(Dispatchers.Default) {
			try {
				username.value = null
				username.value = repository.getAuthProvider()?.getUsername()
			} catch (_: AuthRequiredException) {
			}
		}
	}
}
