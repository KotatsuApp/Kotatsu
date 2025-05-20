package org.koitharu.kotatsu

import android.app.Notification
import android.app.Notification.BigTextStyle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.annotation.RequiresApi
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.strictmode.FragmentStrictMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.koitharu.kotatsu.core.util.ShareHelper
import kotlin.math.absoluteValue
import androidx.fragment.app.strictmode.Violation as FragmentViolation

@RequiresApi(Build.VERSION_CODES.P)
class StrictModeNotifier(
	private val context: Context,
) : StrictMode.OnVmViolationListener, StrictMode.OnThreadViolationListener, FragmentStrictMode.OnViolationListener {

	val executor = Dispatchers.Default.asExecutor()

	private val notificationManager by lazy {
		val nm = checkNotNull(context.getSystemService<NotificationManager>())
		val channel = NotificationChannel(
			CHANNEL_ID,
			context.getString(R.string.strict_mode),
			NotificationManager.IMPORTANCE_LOW,
		)
		nm.createNotificationChannel(channel)
		nm
	}

	override fun onVmViolation(v: Violation) = showNotification(v)

	override fun onThreadViolation(v: Violation) = showNotification(v)

	override fun onViolation(violation: FragmentViolation) = showNotification(violation)

	private fun showNotification(violation: Throwable) = Notification.Builder(context, CHANNEL_ID)
		.setSmallIcon(R.drawable.ic_bug)
		.setContentTitle(context.getString(R.string.strict_mode))
		.setContentText(violation.message)
		.setStyle(
			BigTextStyle()
				.setBigContentTitle(context.getString(R.string.strict_mode))
				.setSummaryText(violation.message)
				.bigText(violation.stackTraceToString()),
		).setShowWhen(true)
		.setContentIntent(
			PendingIntentCompat.getActivity(
				context,
				violation.hashCode(),
				ShareHelper(context).getShareTextIntent(violation.stackTraceToString()),
				0,
				false,
			),
		)
		.setAutoCancel(true)
		.setGroup(CHANNEL_ID)
		.build()
		.let { notificationManager.notify(CHANNEL_ID, violation.hashCode().absoluteValue, it) }

	private companion object {

		const val CHANNEL_ID = "strict_mode"
	}
}
