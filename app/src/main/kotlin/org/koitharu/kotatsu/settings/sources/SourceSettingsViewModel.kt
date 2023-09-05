package org.koitharu.kotatsu.settings.sources

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.HttpUrl
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject

@HiltViewModel
class SourceSettingsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	private val cookieJar: MutableCookieJar,
) : BaseViewModel() {

	val source = savedStateHandle.require<MangaSource>(SourceSettingsFragment.EXTRA_SOURCE)
	val repository = mangaRepositoryFactory.create(source) as RemoteMangaRepository

	val onActionDone = MutableEventFlow<ReversibleAction>()
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

	fun clearCookies() {
		launchLoadingJob(Dispatchers.Default) {
			val url = HttpUrl.Builder()
				.scheme("https")
				.host(repository.domain)
				.build()
			cookieJar.removeCookies(url, null)
			onActionDone.call(ReversibleAction(R.string.cookies_cleared, null))
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
