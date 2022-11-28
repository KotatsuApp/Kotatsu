package org.koitharu.kotatsu.core.os

import android.content.Context
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.koitharu.kotatsu.utils.MediatorStateFlow
import org.koitharu.kotatsu.utils.ext.connectivityManager
import org.koitharu.kotatsu.utils.ext.isNetworkAvailable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkState @Inject constructor(
	@ApplicationContext context: Context,
) : MediatorStateFlow<Boolean>() {

	private val connectivityManager = context.connectivityManager
	private val callback = NetworkCallbackImpl()

	override val initialValue: Boolean
		get() = connectivityManager.isNetworkAvailable

	override fun onActive() {
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

	private inner class NetworkCallbackImpl : NetworkCallback() {

		override fun onAvailable(network: Network) = update()

		override fun onLost(network: Network) = update()

		override fun onUnavailable() = update()

		private fun update() {
			publishValue(connectivityManager.isNetworkAvailable)
		}
	}
}
