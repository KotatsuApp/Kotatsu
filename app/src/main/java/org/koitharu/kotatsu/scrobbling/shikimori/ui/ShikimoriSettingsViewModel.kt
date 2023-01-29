package org.koitharu.kotatsu.scrobbling.shikimori.ui

import androidx.lifecycle.MutableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerUser
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriRepository

class ShikimoriSettingsViewModel @AssistedInject constructor(
	private val repository: ShikimoriRepository,
	@Assisted authCode: String?,
) : BaseViewModel() {

	val authorizationUrl: String
		get() = repository.oauthUrl

	val user = MutableLiveData<ScrobblerUser?>()

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
			repository.cachedUser?.let(user::postValue)
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

		fun create(authCode: String?): ShikimoriSettingsViewModel
	}
}
