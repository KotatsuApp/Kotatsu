package org.koitharu.kotatsu.utils.ext

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.OperationApplicationException
import android.content.SharedPreferences
import android.content.SyncResult
import android.content.pm.ResolveInfo
import android.database.SQLException
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.children
import androidx.core.view.descendants
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.work.CoroutineWorker
import com.google.android.material.elevation.ElevationOverlayProvider
import kotlin.math.roundToLong
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okio.IOException
import org.json.JSONException
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.utils.InternalResourceHelper

val Context.activityManager: ActivityManager?
	get() = getSystemService(ACTIVITY_SERVICE) as? ActivityManager

val Context.connectivityManager: ConnectivityManager
	get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

fun SyncResult.onError(error: Throwable) {
	when (error) {
		is IOException -> stats.numIoExceptions++
		is OperationApplicationException,
		is SQLException,
		-> databaseError = true
		is JSONException -> stats.numParseExceptions++
		else -> if (BuildConfig.DEBUG) throw error
	}
	error.printStackTraceDebug()
}

fun Window.setNavigationBarTransparentCompat(context: Context, elevation: Float = 0F) {
	navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
		!InternalResourceHelper.getBoolean(context, "config_navBarNeedsScrim", true)
	) {
		Color.TRANSPARENT
	} else {
		// Set navbar scrim 70% of navigationBarColor
		ElevationOverlayProvider(context).compositeOverlayIfNeeded(
			context.getResourceColor(android.R.attr.navigationBarColor, 0.7F),
			elevation,
		)
	}
}

val Context.animatorDurationScale: Float
	get() = Settings.Global.getFloat(this.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

fun ViewPropertyAnimator.applySystemAnimatorScale(context: Context): ViewPropertyAnimator = apply {
	this.duration = (this.duration * context.animatorDurationScale).toLong()
}

fun Context.getAnimationDuration(@IntegerRes resId: Int): Long {
	return (resources.getInteger(resId) * animatorDurationScale).roundToLong()
}

inline fun <reified T> ViewGroup.findChild(): T? {
	return children.find { it is T } as? T
}

inline fun <reified T> ViewGroup.findDescendant(): T? {
	return descendants.find { it is T } as? T
}

fun isLowRamDevice(context: Context): Boolean {
	return context.activityManager?.isLowRamDevice ?: false
}

fun scaleUpActivityOptionsOf(view: View): ActivityOptions = ActivityOptions.makeScaleUpAnimation(
	view,
	0,
	0,
	view.width,
	view.height,
)
