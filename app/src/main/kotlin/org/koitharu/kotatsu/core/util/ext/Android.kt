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
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ResolveInfo
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.Window
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.work.CoroutineWorker
import com.google.android.material.elevation.ElevationOverlayProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okio.IOException
import okio.use
import org.json.JSONException
import org.jsoup.internal.StringUtil.StringJoiner
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import kotlin.math.roundToLong
import com.google.android.material.R as materialR

val Context.activityManager: ActivityManager?
	get() = getSystemService(ACTIVITY_SERVICE) as? ActivityManager

val Context.powerManager: PowerManager?
	get() = getSystemService(POWER_SERVICE) as? PowerManager

val Context.connectivityManager: ConnectivityManager
	get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
		is SQLException -> databaseError = true

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
	} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
		val baseColor = context.getThemeColor(android.R.attr.navigationBarColor)
		ColorUtils.setAlphaComponent(baseColor, (Color.alpha(baseColor) * alphaFactor).toInt())
	} else {
		// Set navbar scrim 70% of navigationBarColor
		ElevationOverlayProvider(context).compositeOverlayIfNeeded(
			context.getThemeColor(materialR.attr.colorSurfaceContainer, alphaFactor),
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

fun Fragment.findAppCompatDelegate(): AppCompatDelegate? {
	((this as? DialogFragment)?.dialog as? AppCompatDialog)?.run {
		return delegate
	}
	return parentFragment?.findAppCompatDelegate() ?: (activity as? AppCompatActivity)?.delegate
}

fun Context.checkNotificationPermission(channelId: String?): Boolean {
	val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PERMISSION_GRANTED
	} else {
		NotificationManagerCompat.from(this).areNotificationsEnabled()
	}
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasPermission && channelId != null) {
		val channel = NotificationManagerCompat.from(this).getNotificationChannel(channelId)
		if (channel != null && channel.importance == NotificationManagerCompat.IMPORTANCE_NONE) {
			return false
		}
	}
	return hasPermission
}

suspend fun Bitmap.compressToPNG(output: File) = runInterruptible(Dispatchers.IO) {
	output.outputStream().use { os ->
		if (!compress(Bitmap.CompressFormat.PNG, 100, os)) {
			throw IOException("Failed to encode bitmap into PNG format")
		}
	}
}

fun Context.ensureRamAtLeast(requiredSize: Long) {
	if (ramAvailable < requiredSize) {
		throw IllegalStateException("Not enough free memory")
	}
}

fun WebView.configureForParser(userAgentOverride: String?) = with(settings) {
	javaScriptEnabled = true
	domStorageEnabled = true
	mediaPlaybackRequiresUserGesture = false
	if (WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
		WebViewCompat.setAudioMuted(this@configureForParser, true)
	}
	databaseEnabled = true
	if (userAgentOverride != null) {
		userAgentString = userAgentOverride
	}
}
