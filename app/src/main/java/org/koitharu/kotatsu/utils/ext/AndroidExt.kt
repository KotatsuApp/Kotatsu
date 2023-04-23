package org.koitharu.kotatsu.utils.ext

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.ContextWrapper
import android.content.OperationApplicationException
import android.content.SharedPreferences
import android.content.SyncResult
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.database.SQLException
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.core.app.ActivityOptionsCompat
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
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import kotlin.math.roundToLong

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
			context.getThemeColor(android.R.attr.navigationBarColor, alphaFactor),
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

fun Resources.getLocalesConfig(): LocaleListCompat {
	val tagsList = StringJoiner(",")
	try {
		val xpp: XmlPullParser = getXml(R.xml.locales)
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
