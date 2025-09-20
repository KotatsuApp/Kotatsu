package org.koitharu.kotatsu.core.os

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import coil3.network.ConnectivityChecker
import kotlinx.coroutines.flow.first
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.MediatorStateFlow

class NetworkState(
	private val connectivityManager: ConnectivityManager,
	private val settings: AppSettings,
) : MediatorStateFlow<Boolean>(connectivityManager.isOnline(settings)), ConnectivityChecker {

	private val callback = NetworkCallbackImpl()

	override val value: Boolean
		get() = connectivityManager.isOnline(settings)

	override fun isOnline(): Boolean {
		return connectivityManager.isOnline(settings)
	}

	@Synchronized
	override fun onActive() {
		invalidate()
		val request = NetworkRequest.Builder()
			.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
			.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
			.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
			.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
			.build()
		connectivityManager.registerNetworkCallback(request, callback)
	}

	@Synchronized
	override fun onInactive() {
		connectivityManager.unregisterNetworkCallback(callback)
	}

	fun isMetered(): Boolean {
		return connectivityManager.isActiveNetworkMetered
	}

	fun isDataSaverEnabled(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
		&& connectivityManager.restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED

	fun isRestricted() = isMetered() && isDataSaverEnabled()

	fun isOfflineOrRestricted() = !isOnline() || isRestricted()

	suspend fun awaitForConnection() {
		if (value) {
			return
		}
		first { it }
	}

	private fun invalidate() {
		publishValue(connectivityManager.isOnline(settings))
	}

	private inner class NetworkCallbackImpl : NetworkCallback() {

		override fun onAvailable(network: Network) = invalidate()

		override fun onLost(network: Network) = invalidate()

		override fun onUnavailable() = invalidate()
	}

	private companion object {

		fun ConnectivityManager.isOnline(settings: AppSettings): Boolean {
			if (settings.isOfflineCheckDisabled) {
				return true
			}
			return activeNetwork?.let { isOnline(it) } == true
		}

		private fun ConnectivityManager.isOnline(network: Network): Boolean {
			val capabilities = getNetworkCapabilities(network) ?: return false
			return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
		}
	}
}
