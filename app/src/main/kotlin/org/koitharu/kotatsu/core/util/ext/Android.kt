package org.koitharu.kotatsu.core.util.ext

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.app.LocaleConfig
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.POWER_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.content.OperationApplicationException
import android.content.SyncResult
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ResolveInfo
import android.database.SQLException
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.ViewPropertyAnimator
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.CheckResult
import androidx.annotation.IntegerRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.work.CoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okio.IOException
import okio.use
import org.json.JSONException
import org.jsoup.internal.StringUtil.StringJoiner
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.main.ui.MainActivity
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

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

@CheckResult
fun <I> ActivityResultLauncher<I>.resolve(context: Context, input: I): ResolveInfo? {
	val pm = context.packageManager
	val intent = contract.createIntent(context, input)
	return pm.resolveActivity(intent, 0)
}

@CheckResult
fun <I> ActivityResultLauncher<I>.tryLaunch(
	input: I,
	options: ActivityOptionsCompat? = null,
): Boolean = runCatching {
	launch(input, options)
}.onFailure { e ->
	e.printStackTraceDebug()
}.isSuccess

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
	return activityManager?.isLowRamDevice == true
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

fun Context.getLocalesConfig(): LocaleListCompat {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		LocaleConfig(this).supportedLocales?.let {
			return LocaleListCompat.wrap(it)
		}
	}
	val tagsList = StringJoiner(",")
	try {
		val xpp: XmlPullParser = resources.getXml(R.xml.locales_config)
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
	allowContentAccess = false
	if (userAgentOverride != null) {
		userAgentString = userAgentOverride
	}
	val cookieManager = CookieManager.getInstance()
	cookieManager.setAcceptCookie(true)
	cookieManager.setAcceptThirdPartyCookies(this@configureForParser, true)
}

fun Context.restartApplication() {
	val activity = findActivity()
	val intent = Intent.makeRestartActivityTask(ComponentName(this, MainActivity::class.java))
	startActivity(intent)
	activity?.finishAndRemoveTask()
}

internal inline fun <R> PowerManager?.withPartialWakeLock(tag: String, body: (PowerManager.WakeLock?) -> R): R {
	val wakeLock = newPartialWakeLock(tag)
	return try {
		wakeLock?.acquire(TimeUnit.HOURS.toMillis(1))
		body(wakeLock)
	} finally {
		wakeLock?.release()
	}
}

private fun PowerManager?.newPartialWakeLock(tag: String): PowerManager.WakeLock? {
	return if (this != null && isWakeLockLevelSupported(PowerManager.PARTIAL_WAKE_LOCK)) {
		newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
	} else {
		null
	}
}

fun Context.copyToClipboard(label: String, content: String) {
	val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
	clipboardManager.setPrimaryClip(ClipData.newPlainText(label, content))
}
