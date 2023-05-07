package org.koitharu.kotatsu.sync.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.sync.data.SyncAuthApi
import org.koitharu.kotatsu.sync.domain.SyncAuthResult
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.ifNullOrEmpty
import javax.inject.Inject

@HiltViewModel
class SyncAuthViewModel @Inject constructor(
	@ApplicationContext context: Context,
	private val api: SyncAuthApi,
) : BaseViewModel() {

	val onTokenObtained = SingleLiveEvent<SyncAuthResult>()
	val host = MutableLiveData("")

	private val defaultHost = context.getString(R.string.sync_host_default)

	fun obtainToken(email: String, password: String) {
		val hostValue = host.value.ifNullOrEmpty { defaultHost }
		launchLoadingJob(Dispatchers.Default) {
			val token = api.authenticate(hostValue, email, password)
			val result = SyncAuthResult(host.value.orEmpty(), email, password, token)
			onTokenObtained.emitCall(result)
		}
	}
}
