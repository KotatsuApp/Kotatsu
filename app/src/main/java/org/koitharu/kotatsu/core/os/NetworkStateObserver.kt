package org.koitharu.kotatsu.core.os

import android.content.Context
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.koitharu.kotatsu.utils.ext.connectivityManager
import org.koitharu.kotatsu.utils.ext.isNetworkAvailable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateObserver @Inject constructor(
	@ApplicationContext context: Context,
) : StateFlow<Boolean> {

	private val connectivityManager = context.connectivityManager

	override val replayCache: List<Boolean>
		get() = listOf(value)

	override val value: Boolean
		get() = connectivityManager.isNetworkAvailable

	override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
		collector.emit(value)
		while (true) {
			observeImpl().collect(collector)
		}
	}

	private fun observeImpl() = callbackFlow<Boolean> {
		val request = NetworkRequest.Builder().build()
		val callback = FlowNetworkCallback(this)
		connectivityManager.registerNetworkCallback(request, callback)
		awaitClose {
			connectivityManager.unregisterNetworkCallback(callback)
		}
	}

	private inner class FlowNetworkCallback(
		private val producerScope: ProducerScope<Boolean>,
	) : NetworkCallback() {

		private var prevValue = value

		override fun onAvailable(network: Network) = update()

		override fun onLost(network: Network) = update()

		override fun onUnavailable() = update()

		private fun update() {
			val newValue = connectivityManager.isNetworkAvailable
			if (newValue != prevValue) {
				producerScope.trySendBlocking(newValue).onSuccess {
					prevValue = newValue
				}
			}
		}
	}
}
