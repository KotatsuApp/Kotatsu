package org.koitharu.kotatsu.core.util.ext

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

val Context.connectivityManager: ConnectivityManager
	get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

fun ConnectivityManager.isOnline(): Boolean {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		activeNetwork?.let { isOnline(it) } ?: false
	} else {
		@Suppress("DEPRECATION")
		activeNetworkInfo?.isConnected == true
	}
}

private fun ConnectivityManager.isOnline(network: Network): Boolean {
	val capabilities = getNetworkCapabilities(network)
	return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
