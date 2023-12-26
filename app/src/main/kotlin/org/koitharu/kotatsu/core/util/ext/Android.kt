package org.koitharu.kotatsu.core.util.ext

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.app.ActivityOptions
import android.app.LocaleConfig
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.POWER_SERVICE
import android.content.ContextWrapper
import android.content.OperationApplicationException
import android.content.SharedPreferences
import android.content.SyncResult
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.SQLException
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.Window
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.work.CoroutineWorker
import com.google.android.material.elevation.ElevationOverlayProvider
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
import org.jsoup.internal.StringUtil.StringJoiner
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import kotlin.math.roundToLong

val Context.activityManager: ActivityManager?
	get() = getSystemService(ACTIVITY_SERVICE) as? ActivityManager

val Context.powerManager: PowerManager?
	get() = getSystemService(POWER_SERVICE) as? PowerManager

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

fun <I> ActivityResultLauncher<I>.tryLaunch(
	input: I,
	options: ActivityOptionsCompat? = null,
): Boolean = runCatching {
	launch(input, options)
}.onFailure { e ->
	e.printStackTraceDebug()
}.isSuccess

fun SharedPreferences.observe(): Flow<String?> = callbackFlow {
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

fun Lifecycle.postDelayed(delay: Long, runnable: Runnable) {
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

fun Window.setNavigationBarTransparentCompat(context: Context, elevation: Float, alphaFactor: Float = 0.7f) {
	navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
		!context.getSystemBoolean("config_navBarNeedsScrim", true)
	) {
		Color.TRANSPARENT
	} else {
		// Set navbar scrim 70% of navigationBarColor
		ElevationOverlayProvider(context).compositeOverlayIfNeeded(
			context.getThemeColor(R.attr.m3ColorBottomMenuBackground, alphaFactor),
			elevation,
		)
	}
}

val Context.animatorDurationScale: Float
	get() = Settings.Global.getFloat(this.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

val Context.isAnimationsEnabled: Boolean
	get() = animatorDurationScale > 0f

fun ViewPropertyAnimator.applySystemAnimatorScale(context: Context): ViewPropertyAnimator = apply {
	this.duration = (this.duration * context.animatorDurationScale).toLong()
}

fun Context.getAnimationDuration(@IntegerRes resId: Int): Long {
	return (resources.getInteger(resId) * animatorDurationScale).roundToLong()
}

fun Context.isLowRamDevice(): Boolean {
	return activityManager?.isLowRamDevice ?: false
}

fun Context.isPowerSaveMode(): Boolean {
	return powerManager?.isPowerSaveMode == true
}

val Context.ramAvailable: Long
	get() {
		val result = MemoryInfo()
		activityManager?.getMemoryInfo(result)
		return result.availMem
	}

fun scaleUpActivityOptionsOf(view: View): Bundle? = if (view.context.isAnimationsEnabled) {
	ActivityOptions.makeScaleUpAnimation(
		view,
		0,
		0,
		view.width,
		view.height,
	).toBundle()
} else {
	null
}

@SuppressLint("DiscouragedApi")
fun Context.getLocalesConfig(): LocaleListCompat {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		LocaleConfig(this).supportedLocales?.let {
			return LocaleListCompat.wrap(it)
		}
	}
	val tagsList = StringJoiner(",")
	try {
		val resId = resources.getIdentifier("_generated_res_locale_config", "xml", packageName)
		val xpp: XmlPullParser = resources.getXml(resId)
		while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
			if (xpp.eventType == XmlPullParser.START_TAG) {
				if (xpp.name == "locale") {
					tagsList.add(xpp.getAttributeValue(0))
				}
			}
			xpp.next()
		}
	} catch (e: XmlPullParserException) {
		e.printStackTraceDebug()
	} catch (e: IOException) {
		e.printStackTraceDebug()
	}
	return LocaleListCompat.forLanguageTags(tagsList.complete())
}

fun Context.findActivity(): Activity? = when (this) {
	is Activity -> this
	is ContextWrapper -> baseContext.findActivity()
	else -> null
}

inline fun Activity.catchingWebViewUnavailability(block: () -> Unit): Boolean {
	return try {
		block()
		true
	} catch (e: Exception) {
		if (e.isWebViewUnavailable()) {
			Toast.makeText(this, R.string.web_view_unavailable, Toast.LENGTH_LONG).show()
			finishAfterTransition()
			false
		} else {
			throw e
		}
	}
}

fun Context.checkNotificationPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
	ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
} else {
	NotificationManagerCompat.from(this).areNotificationsEnabled()
}
