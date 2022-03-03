package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun ConnectivityManager.waitForNetwork(): Network {
	val request = NetworkRequest.Builder().build()
	return suspendCancellableCoroutine<Network> { cont ->
		val callback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				cont.resume(network)
			}
		}
		registerNetworkCallback(request, callback)
		cont.invokeOnCancellation {
			unregisterNetworkCallback(callback)
		}
	}
}

inline fun buildAlertDialog(context: Context, block: MaterialAlertDialogBuilder.() -> Unit): AlertDialog {
	return MaterialAlertDialogBuilder(context).apply(block).create()
}

fun <T : Parcelable> Bundle.requireParcelable(key: String): T {
	return checkNotNull(getParcelable(key)) {
		"Value for key $key not found"
	}
}