package org.koitharu.kotatsu.browser.cloudflare

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import coil3.EventListener
import coil3.Extras
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.parsers.model.MangaSource

class CaptchaNotifier(
	private val context: Context,
) : EventListener() {

	fun notify(exception: CloudFlareProtectedException) {
		if (!context.checkNotificationPermission(CHANNEL_ID)) {
			return
		}
		val manager = NotificationManagerCompat.from(context)
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
			.setName(context.getString(R.string.captcha_required))
			.setShowBadge(true)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		manager.createNotificationChannel(channel)

		val intent = CloudFlareActivity.newIntent(context, exception)
			.setData(exception.url.toUri())
		val notification = NotificationCompat.Builder(context, CHANNEL_ID)
			.setContentTitle(channel.name)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(NotificationCompat.DEFAULT_SOUND)
			.setSmallIcon(android.R.drawable.stat_notify_error)
			.setGroup(GROUP_CAPTCHA)
			.setAutoCancel(true)
			.setVisibility(
				if (exception.source?.isNsfw() == true) {
					NotificationCompat.VISIBILITY_SECRET
				} else {
					NotificationCompat.VISIBILITY_PUBLIC
				},
			)
			.setContentText(
				context.getString(
					R.string.captcha_required_summary,
					exception.source?.getTitle(context) ?: context.getString(R.string.app_name),
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
		manager.notify(TAG, exception.source.hashCode(), notification.build())
	}

	fun dismiss(source: MangaSource) {
		NotificationManagerCompat.from(context).cancel(TAG, source.hashCode())
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		super.onError(request, result)
		val e = result.throwable
		if (e is CloudFlareProtectedException && request.extras[ignoreCaptchaKey] != true) {
			notify(e)
		}
	}

	companion object {

		fun ImageRequest.Builder.ignoreCaptchaErrors() = apply {
			extras[ignoreCaptchaKey] = true
		}

		val ignoreCaptchaKey = Extras.Key(false)

		private const val CHANNEL_ID = "captcha"
		private const val TAG = CHANNEL_ID
		private const val GROUP_CAPTCHA = "org.koitharu.kotatsu.CAPTCHA"
		private const val SETTINGS_ACTION_CODE = 3
	}
}
