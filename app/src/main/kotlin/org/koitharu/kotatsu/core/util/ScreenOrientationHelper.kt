package org.koitharu.kotatsu.core.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Handler
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

class ScreenOrientationHelper(private val activity: Activity) {

	val isAutoRotationEnabled: Boolean
		get() = Settings.System.getInt(
			activity.contentResolver,
			Settings.System.ACCELEROMETER_ROTATION,
			0,
		) == 1

	var isLandscape: Boolean
		get() = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
		set(value) {
			activity.requestedOrientation = if (value) {
				ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
			} else {
				ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
			}
		}

	fun toggleOrientation() {
		isLandscape = !isLandscape
	}

	fun observeAutoOrientation() = callbackFlow {
		val observer = object : ContentObserver(Handler(activity.mainLooper)) {
			override fun onChange(selfChange: Boolean) {
				trySendBlocking(isAutoRotationEnabled)
			}
		}
		activity.contentResolver.registerContentObserver(
			Settings.System.CONTENT_URI, true, observer,
		)
		awaitClose {
			activity.contentResolver.unregisterContentObserver(observer)
		}
	}.onStart {
		emit(isAutoRotationEnabled)
	}.distinctUntilChanged()
}
