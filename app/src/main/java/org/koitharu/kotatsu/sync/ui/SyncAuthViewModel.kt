package org.koitharu.kotatsu.sync.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.sync.data.SyncAuthApi
import org.koitharu.kotatsu.sync.domain.SyncAuthResult
import org.koitharu.kotatsu.utils.SingleLiveEvent

@HiltViewModel
class SyncAuthViewModel @Inject constructor(
	private val api: SyncAuthApi,
) : BaseViewModel() {

	val onTokenObtained = SingleLiveEvent<SyncAuthResult>()

	fun obtainToken(email: String, password: String) {
		launchLoadingJob(Dispatchers.Default) {
			val token = api.authenticate(email, password)
			val result = SyncAuthResult(email, password, token)
			onTokenObtained.postCall(result)
		}
	}
}
