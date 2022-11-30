package org.koitharu.kotatsu.core.os

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import kotlinx.coroutines.flow.first
import org.koitharu.kotatsu.utils.MediatorStateFlow
import org.koitharu.kotatsu.utils.ext.isNetworkAvailable

class NetworkState(
	private val connectivityManager: ConnectivityManager,
) : MediatorStateFlow<Boolean>(connectivityManager.isNetworkAvailable) {

	private val callback = NetworkCallbackImpl()

	override fun onActive() {
		invalidate()
		val request = NetworkRequest.Builder().build()
		connectivityManager.registerNetworkCallback(request, callback)
	}

	override fun onInactive() {
		connectivityManager.unregisterNetworkCallback(callback)
	}

	suspend fun awaitForConnection() {
		if (value) {
			return
		}
		first { it }
	}

	private fun invalidate() {
		publishValue(connectivityManager.isNetworkAvailable)
	}

	private inner class NetworkCallbackImpl : NetworkCallback() {

		override fun onAvailable(network: Network) = invalidate()

		override fun onLost(network: Network) = invalidate()

		override fun onUnavailable() = invalidate()
	}
}
