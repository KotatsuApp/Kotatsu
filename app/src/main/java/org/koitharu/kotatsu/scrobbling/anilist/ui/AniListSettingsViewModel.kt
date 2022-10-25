package org.koitharu.kotatsu.scrobbling.anilist.ui

import androidx.lifecycle.MutableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.scrobbling.anilist.data.AniListRepository
import org.koitharu.kotatsu.scrobbling.anilist.data.model.AniListUser

class AniListSettingsViewModel @AssistedInject constructor(
	private val repository: AniListRepository,
	@Assisted authCode: String?,
) : BaseViewModel() {

	val authorizationUrl: String
		get() = repository.oauthUrl

	val user = MutableLiveData<AniListUser?>()

	init {
		if (authCode != null) {
			authorize(authCode)
		} else {
			loadUser()
		}
	}

	fun logout() {
		launchJob(Dispatchers.Default) {
			repository.logout()
			user.postValue(null)
		}
	}

	private fun loadUser() = launchJob(Dispatchers.Default) {
		val userModel = if (repository.isAuthorized) {
			repository.getCachedUser()?.let(user::postValue)
			repository.loadUser()
		} else {
			null
		}
		user.postValue(userModel)
	}

	private fun authorize(code: String) = launchJob(Dispatchers.Default) {
		repository.authorize(code)
		user.postValue(repository.loadUser())
	}

	@AssistedFactory
	interface Factory {

		fun create(authCode: String?): AniListSettingsViewModel
	}
}
