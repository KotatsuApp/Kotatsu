package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.work.CoroutineWorker
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

val Context.connectivityManager: ConnectivityManager
	get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

suspend fun ConnectivityManager.waitForNetwork(): Network {
	val request = NetworkRequest.Builder().build()
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		// fast path
		activeNetwork?.let { return it }
	}
	return suspendCancellableCoroutine { cont ->
		val callback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				unregisterNetworkCallback(this)
				if (cont.isActive) {
					cont.resume(network)
				}
			}
		}
		registerNetworkCallback(request, callback)
		cont.invokeOnCancellation {
			unregisterNetworkCallback(callback)
		}
	}
}

fun String.toUriOrNull() = if (isEmpty()) null else Uri.parse(this)

suspend fun CoroutineWorker.trySetForeground(): Boolean = runCatching {
	val info = getForegroundInfo()
	setForeground(info)
}.isSuccess

fun <I> ActivityResultLauncher<I>.resolve(context: Context, input: I): ResolveInfo? {
	val pm = context.packageManager
	val intent = contract.createIntent(context, input)
	return pm.resolveActivity(intent, 0)
}

fun <I> ActivityResultLauncher<I>.tryLaunch(input: I, options: ActivityOptionsCompat? = null): Boolean {
	return runCatching {
		launch(input, options)
	}.isSuccess
}