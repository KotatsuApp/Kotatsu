package org.koitharu.kotatsu.reader.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Handler
import android.provider.Settings
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@ActivityScoped
class ScreenOrientationHelper @Inject constructor(private val activity: Activity) {

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

	var isLocked: Boolean
		get() = activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LOCKED
		set(value) {
			activity.requestedOrientation = if (value) {
				ActivityInfo.SCREEN_ORIENTATION_LOCKED
			} else {
				ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
			}
		}

	fun init(orientation: Int) {
		if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
			// https://developer.android.com/reference/android/R.attr.html#screenOrientation
			activity.requestedOrientation = orientation
		}
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
		.conflate()
}
