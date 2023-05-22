package org.koitharu.kotatsu.scrobbling.common.ui.config

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.SingleLiveEvent
import org.koitharu.kotatsu.core.util.asFlowLiveData
import org.koitharu.kotatsu.core.util.ext.emitValue
import org.koitharu.kotatsu.core.util.ext.onFirst
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus
import javax.inject.Inject

@HiltViewModel
class ScrobblerConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
) : BaseViewModel() {

	private val scrobblerService = getScrobblerService(savedStateHandle)
	private val scrobbler = scrobblers.first { it.scrobblerService == scrobblerService }

	val titleResId = scrobbler.scrobblerService.titleResId

	val user = MutableLiveData<ScrobblerUser?>(null)
	val onLoggedOut = SingleLiveEvent<Unit>()

	val content = scrobbler.observeAllScrobblingInfo()
		.onStart { loadingCounter.increment() }
		.onFirst { loadingCounter.decrement() }
		.catch { errorEvent.postCall(it) }
		.map { buildContentList(it) }
		.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	init {
		scrobbler.user
			.onEach { user.emitValue(it) }
			.launchIn(viewModelScope + Dispatchers.Default)
	}

	fun onAuthCodeReceived(authCode: String) {
		launchLoadingJob(Dispatchers.Default) {
			val newUser = scrobbler.authorize(authCode)
			user.emitValue(newUser)
		}
	}

	fun logout() {
		launchLoadingJob(Dispatchers.Default) {
			scrobbler.logout()
			user.emitValue(null)
			onLoggedOut.emitCall(Unit)
		}
	}

	private fun buildContentList(list: List<ScrobblingInfo>): List<ListModel> {
		if (list.isEmpty()) {
			return listOf(
				EmptyState(
					icon = R.drawable.ic_empty_history,
					textPrimary = R.string.nothing_here,
					textSecondary = R.string.scrobbling_empty_hint,
					actionStringRes = 0,
				),
			)
		}
		val grouped = list.groupBy { it.status }
		val statuses = enumValues<ScrobblingStatus>()
		val result = ArrayList<ListModel>(list.size + statuses.size)
		for (st in statuses) {
			val subList = grouped[st]
			if (subList.isNullOrEmpty()) {
				continue
			}
			result.add(st)
			result.addAll(subList)
		}
		return result
	}

	private fun getScrobblerService(
		savedStateHandle: SavedStateHandle,
	): ScrobblerService {
		val serviceId = savedStateHandle.get<Int>(ScrobblerConfigActivity.EXTRA_SERVICE_ID) ?: 0
		if (serviceId != 0) {
			return enumValues<ScrobblerService>().first { it.id == serviceId }
		}
		val uri = savedStateHandle.require<Uri>(BaseActivity.EXTRA_DATA)
		return when (uri.host) {
			ScrobblerConfigActivity.HOST_SHIKIMORI_AUTH -> ScrobblerService.SHIKIMORI
			ScrobblerConfigActivity.HOST_ANILIST_AUTH -> ScrobblerService.ANILIST
			ScrobblerConfigActivity.HOST_MAL_AUTH -> ScrobblerService.MAL
			else -> error("Wrong scrobbler uri: $uri")
		}
	}
}
