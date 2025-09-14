package org.koitharu.kotatsu.core.exceptions.resolve

import android.Manifest
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.CheckResult
import androidx.annotation.RequiresPermission
import androidx.collection.MutableScatterMap
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import androidx.lifecycle.coroutineScope
import coil3.EventListener
import coil3.Extras
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowConversionToBitmap
import coil3.request.allowHardware
import coil3.request.lifecycle
import coil3.size.Scale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.LocalizedAppContext
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.exceptions.CloudFlareException
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.UnknownMangaSource
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.network.webview.WebViewExecutor
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.getNotificationIconSize
import org.koitharu.kotatsu.core.util.ext.goAsync
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.core.util.ext.toBitmapOrNull
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.network.CloudFlareHelper
import org.koitharu.kotatsu.parsers.util.mapToArray
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class CaptchaHandler @Inject constructor(
	@LocalizedAppContext private val context: Context,
	private val databaseProvider: Provider<MangaDatabase>,
	private val coilProvider: Provider<ImageLoader>,
	private val webViewExecutor: WebViewExecutor,
) : EventListener() {

	private val exceptionMap = MutableScatterMap<MangaSource, CloudFlareProtectedException>()
	private val mutex = Mutex()

	@CheckResult
	suspend fun handle(exception: CloudFlareException): Boolean = handleException(exception.source, exception, true)

	suspend fun discard(source: MangaSource) {
		handleException(source, null, true)
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		super.onError(request, result)
		val e = result.throwable
		if (e is CloudFlareException) {
			val scope = request.lifecycle?.coroutineScope ?: processLifecycleScope
			scope.launch {
				if (
					handleException(
						source = e.source,
						exception = e,
						notify = request.extras[suppressCaptchaKey] != true,
					)
				) {
					coilProvider.get().enqueue(request) // TODO check if ok
				}
			}
		}
	}

	private suspend fun handleException(
		source: MangaSource,
		exception: CloudFlareException?,
		notify: Boolean,
	): Boolean = withContext(Dispatchers.Default) {
		if (source == UnknownMangaSource) {
			return@withContext false
		}
		if (exception != null && webViewExecutor.tryResolveCaptcha(exception, RESOLVE_TIMEOUT)) {
			return@withContext true
		}
		mutex.withLock {
			var removedException: CloudFlareProtectedException? = null
			if (exception is CloudFlareProtectedException) {
				exceptionMap[source] = exception
			} else {
				removedException = exceptionMap.remove(source)
			}
			val dao = databaseProvider.get().getSourcesDao()
			dao.setCfState(source.name, exception?.state ?: CloudFlareHelper.PROTECTION_NOT_DETECTED)

			if (notify && context.checkNotificationPermission(CHANNEL_ID)) {
				val exceptions = dao.findAllCaptchaRequired().mapNotNull {
					it.source.toMangaSourceOrNull()
				}.filterNot {
					SourceSettings(context, it).isCaptchaNotificationsDisabled
				}.mapNotNull {
					exceptionMap[it]
				}
				if (removedException != null) {
					NotificationManagerCompat.from(context).cancel(TAG, removedException.source.hashCode())
				}
				notify(exceptions)
			}
		}
		false
	}

	@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
	private suspend fun notify(exceptions: List<CloudFlareProtectedException>) {
		val manager = NotificationManagerCompat.from(context)
		val channel = NotificationChannelCompat.Builder(
			CHANNEL_ID,
			NotificationManagerCompat.IMPORTANCE_LOW,
		)
			.setName(context.getString(R.string.captcha_required))
			.setShowBadge(true)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		manager.createNotificationChannel(channel)

		coroutineScope {
			exceptions.map {
				async { it to buildNotification(it) }
			}.awaitAll()
		}.forEach { (exception, notification) ->
			manager.notify(TAG, exception.source.hashCode(), notification)
		}
		if (exceptions.size > 1) {
			val groupNotification = NotificationCompat.Builder(context, CHANNEL_ID)
				.setGroupSummary(true)
				.setContentTitle(context.getString(R.string.captcha_required))
				.setPriority(NotificationCompat.PRIORITY_LOW)
				.setDefaults(0)
				.setOnlyAlertOnce(true)
				.setSmallIcon(R.drawable.ic_bot)
				.setGroup(GROUP_CAPTCHA)
				.setContentIntent(
					PendingIntentCompat.getActivities(
						context, GROUP_NOTIFICATION_ID,
						exceptions.mapToArray { e ->
							AppRouter.cloudFlareResolveIntent(context, e)
						},
						0, false,
					),
				)
				.setContentText(
					context.getString(
						R.string.captcha_required_summary, context.getString(R.string.app_name),
					),
				)
				.setVisibility(
					if (exceptions.any { it.source.isNsfw() }) {
						NotificationCompat.VISIBILITY_SECRET
					} else {
						NotificationCompat.VISIBILITY_PUBLIC
					},
				)
			manager.notify(TAG, GROUP_NOTIFICATION_ID, groupNotification.build())
		} else {
			manager.cancel(TAG, GROUP_NOTIFICATION_ID)
		}
	}

	private suspend fun buildNotification(exception: CloudFlareProtectedException): Notification {
		val intent = AppRouter.cloudFlareResolveIntent(context, exception)
		val discardIntent = Intent(ACTION_DISCARD)
			.putExtra(AppRouter.KEY_SOURCE, exception.source.name)
			.setData("source://${exception.source.name}".toUri())
		val notification = NotificationCompat.Builder(context, CHANNEL_ID)
			.setContentTitle(context.getString(R.string.captcha_required))
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setDefaults(0)
			.setSmallIcon(R.drawable.ic_bot)
			.setGroup(GROUP_CAPTCHA)
			.setOnlyAlertOnce(true)
			.setAutoCancel(true)
			.setDeleteIntent(PendingIntentCompat.getBroadcast(context, 0, discardIntent, 0, false))
			.setLargeIcon(getFavicon(exception.source))
			.setVisibility(
				if (exception.source.isNsfw()) {
					NotificationCompat.VISIBILITY_SECRET
				} else {
					NotificationCompat.VISIBILITY_PUBLIC
				},
			)
			.setContentText(
				context.getString(
					R.string.captcha_required_summary,
					exception.source.getTitle(context),
				),
			)
			.setContentIntent(PendingIntentCompat.getActivity(context, 0, intent, 0, false))
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val actionIntent = PendingIntentCompat.getActivity(
				context, SETTINGS_ACTION_CODE,
				Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
					.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
					.putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID),
				0, false,
			)
			notification.addAction(
				R.drawable.ic_settings,
				context.getString(R.string.notifications_settings),
				actionIntent,
			)
		}
		return notification.build()
	}

	private fun String.toMangaSourceOrNull() = MangaSource(this).takeUnless { it == UnknownMangaSource }

	private suspend fun getFavicon(source: MangaSource) = runCatchingCancellable {
		coilProvider.get().execute(
			ImageRequest.Builder(context)
				.data(source.faviconUri())
				.allowHardware(false)
				.allowConversionToBitmap(true)
				.suppressCaptchaErrors()
				.mangaSourceExtra(source)
				.size(context.resources.getNotificationIconSize())
				.scale(Scale.FILL)
				.build(),
		).toBitmapOrNull()
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrNull()

	@AndroidEntryPoint
	class DiscardReceiver : BroadcastReceiver() {

		@Inject
		lateinit var captchaHandler: CaptchaHandler

		override fun onReceive(context: Context?, intent: Intent?) {
			val sourceName = intent?.getStringExtra(AppRouter.KEY_SOURCE) ?: return
			goAsync {
				captchaHandler.handleException(MangaSource(sourceName), exception = null, notify = false)
			}
		}
	}

	companion object {

		fun ImageRequest.Builder.suppressCaptchaErrors() = apply {
			extras[suppressCaptchaKey] = true
		}

		private val suppressCaptchaKey = Extras.Key(false)

		private const val CHANNEL_ID = "captcha"
		private const val TAG = CHANNEL_ID
		private const val GROUP_CAPTCHA = "org.koitharu.kotatsu.CAPTCHA"
		private const val GROUP_NOTIFICATION_ID = 34
		private const val SETTINGS_ACTION_CODE = 3
		private const val ACTION_DISCARD = "org.koitharu.kotatsu.CAPTCHA_DISCARD"
		private const val RESOLVE_TIMEOUT = 20_000L
	}
}
