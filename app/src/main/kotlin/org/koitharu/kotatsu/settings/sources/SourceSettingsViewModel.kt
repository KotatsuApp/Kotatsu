package org.koitharu.kotatsu.settings.sources

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.HttpUrl
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.ParserMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import javax.inject.Inject

@HiltViewModel
class SourceSettingsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	private val cookieJar: MutableCookieJar,
	private val mangaSourcesRepository: MangaSourcesRepository,
) : BaseViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

	val source = MangaSource(savedStateHandle.get<String>(SourceSettingsFragment.EXTRA_SOURCE))
	val repository = mangaRepositoryFactory.create(source)

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val username = MutableStateFlow<String?>(null)
	val browserUrl = MutableStateFlow<String?>(null)
	val isEnabled = mangaSourcesRepository.observeIsEnabled(source)
	private var usernameLoadJob: Job? = null

	init {
		when (repository) {
			is ParserMangaRepository -> {
				browserUrl.value = "https://${repository.domain}"
				repository.getConfig().subscribe(this)
				loadUsername(repository.getAuthProvider())
			}
		}
	}

	override fun onCleared() {
		when (repository) {
			is ParserMangaRepository -> {
				repository.getConfig().unsubscribe(this)
			}
		}
		super.onCleared()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (repository is CachingMangaRepository) {
			if (key != SourceSettings.KEY_SLOWDOWN && key != SourceSettings.KEY_SORT_ORDER) {
				repository.invalidateCache()
			}
		}
		if (repository is ParserMangaRepository) {
			if (key == AppSettings.KEY_OPEN_BROWSER) {
				browserUrl.value = "https://${repository.domain}"
			}
		}
	}

	fun onResume() {
		if (usernameLoadJob?.isActive != true && repository is ParserMangaRepository) {
			loadUsername(repository.getAuthProvider())
		}
	}

	fun clearCookies() {
		if (repository !is ParserMangaRepository) return
		launchLoadingJob(Dispatchers.Default) {
			val url = HttpUrl.Builder()
				.scheme("https")
				.host(repository.domain)
				.build()
			cookieJar.removeCookies(url, null)
			onActionDone.call(ReversibleAction(R.string.cookies_cleared, null))
			loadUsername(repository.getAuthProvider())
		}
	}

	fun setEnabled(value: Boolean) {
		launchJob(Dispatchers.Default) {
			mangaSourcesRepository.setSourcesEnabled(setOf(source), value)
		}
	}

	private fun loadUsername(authProvider: MangaParserAuthProvider?) {
		launchLoadingJob(Dispatchers.Default) {
			try {
				username.value = null
				username.value = authProvider?.getUsername()
			} catch (_: AuthRequiredException) {
			}
		}
	}
}
