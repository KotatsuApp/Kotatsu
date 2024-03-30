package org.koitharu.kotatsu.browser.cloudflare

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import coil.EventListener
import coil.request.ErrorResult
import coil.request.ImageRequest
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource

class CaptchaNotifier(
	private val context: Context,
) : EventListener {

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

		val intent = CloudFlareActivity.newIntent(context, exception.url, exception.headers)
			.setData(exception.url.toUri())
		val notification = NotificationCompat.Builder(context, CHANNEL_ID)
			.setContentTitle(channel.name)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(NotificationCompat.DEFAULT_SOUND)
			.setSmallIcon(android.R.drawable.stat_notify_error)
			.setAutoCancel(true)
			.setVisibility(
				if (exception.source?.contentType == ContentType.HENTAI) {
					NotificationCompat.VISIBILITY_SECRET
				} else {
					NotificationCompat.VISIBILITY_PUBLIC
				},
			)
			.setContentText(
				context.getString(
					R.string.captcha_required_summary,
					exception.source?.title ?: context.getString(R.string.app_name),
				),
			)
			.setContentIntent(PendingIntentCompat.getActivity(context, 0, intent, 0, false))
			.build()
		manager.notify(TAG, exception.source.hashCode(), notification)
	}

	fun dismiss(source: MangaSource) {
		NotificationManagerCompat.from(context).cancel(TAG, source.hashCode())
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		super.onError(request, result)
		val e = result.throwable
		if (e is CloudFlareProtectedException && request.parameters.value<Boolean>(PARAM_IGNORE_CAPTCHA) != true) {
			notify(e)
		}
	}

	companion object {

		fun ImageRequest.Builder.ignoreCaptchaErrors() = setParameter(
			key = PARAM_IGNORE_CAPTCHA,
			value = true,
			memoryCacheKey = null,
		)

		private const val PARAM_IGNORE_CAPTCHA = "ignore_captcha"
		private const val CHANNEL_ID = "captcha"
		private const val TAG = CHANNEL_ID
	}
}
