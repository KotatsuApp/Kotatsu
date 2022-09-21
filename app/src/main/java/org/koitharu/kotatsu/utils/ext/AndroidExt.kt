package org.koitharu.kotatsu.utils.ext

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.work.CoroutineWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

val Context.connectivityManager: ConnectivityManager
	get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

val Context.activityManager: ActivityManager?
	get() = getSystemService(ACTIVITY_SERVICE) as? ActivityManager

fun String.toUriOrNull() = if (isEmpty()) null else Uri.parse(this)

suspend fun CoroutineWorker.trySetForeground(): Boolean = runCatchingCancellable {
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

fun SharedPreferences.observe() = callbackFlow<String> {
	val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		trySendBlocking(key)
	}
	registerOnSharedPreferenceChangeListener(listener)
	awaitClose {
		unregisterOnSharedPreferenceChangeListener(listener)
	}
}

fun <T> SharedPreferences.observe(key: String, valueProducer: suspend () -> T): Flow<T> = flow {
	emit(valueProducer())
	observe().collect { upstreamKey ->
		if (upstreamKey == key) {
			emit(valueProducer())
		}
	}
}.distinctUntilChanged()

fun Lifecycle.postDelayed(runnable: Runnable, delay: Long) {
	coroutineScope.launch {
		delay(delay)
		runnable.run()
	}
}

fun isLowRamDevice(context: Context): Boolean {
	return context.activityManager?.isLowRamDevice ?: false
}