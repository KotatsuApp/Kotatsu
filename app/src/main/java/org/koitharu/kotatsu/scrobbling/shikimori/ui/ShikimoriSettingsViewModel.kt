package org.koitharu.kotatsu.scrobbling.shikimori.ui

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriRepository
import org.koitharu.kotatsu.scrobbling.shikimori.data.model.ShikimoriUser

class ShikimoriSettingsViewModel(
	private val repository: ShikimoriRepository,
	authCode: String?,
) : BaseViewModel() {

	val authorizationUrl: String
		get() = repository.oauthUrl

	val user = MutableLiveData<ShikimoriUser?>()

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
}