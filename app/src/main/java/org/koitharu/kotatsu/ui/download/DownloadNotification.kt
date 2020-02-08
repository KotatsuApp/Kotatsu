package org.koitharu.kotatsu.ui.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import kotlin.math.roundToInt

class DownloadNotification(private val context: Context) {

	private val builder = NotificationCompat.Builder(context, CHANNEL_ID)
	private val manager =
		context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	init {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				context.getString(R.string.downloads),
				NotificationManager.IMPORTANCE_LOW
			)
			channel.enableVibration(false)
			manager.createNotificationChannel(channel)
		}
		builder.setOnlyAlertOnce(true)
	}

	fun fillFrom(manga: Manga) {
		builder.setContentTitle(manga.title)
		builder.setContentText(context.getString(R.string.manga_downloading_))
		builder.setProgress(1, 0, true)
		builder.setSmallIcon(android.R.drawable.stat_sys_download)
		builder.setSubText(context.getText(R.string.preparing_))
		builder.setLargeIcon(null)
	}

	fun setLargeIcon(icon: Drawable?) {
		builder.setLargeIcon((icon as? BitmapDrawable)?.bitmap)
	}

	fun setProgress(chaptersTotal: Int, pagesTotal: Int, chapter: Int, page: Int) {
		val max = chaptersTotal * PROGRESS_STEP
		val progress =
			chapter * PROGRESS_STEP + (page / pagesTotal.toFloat() * PROGRESS_STEP).roundToInt()
		val percent = (progress / max.toFloat() * 100).roundToInt()
		builder.setProgress(max, progress, false)
		builder.setSubText("$percent%")
	}

	fun setPostProcessing() {
		builder.setProgress(1, 0, true)
		builder.setSubText(context.getString(R.string.processing_))
	}

	fun setDone() {
		builder.setProgress(0, 0, false)
		builder.setContentText(context.getString(R.string.download_complete))
		builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
		builder.setSubText(null)
	}

	fun update(id: Int = NOTIFICATION_ID) {
		manager.notify(id, builder.build())
	}

	operator fun invoke(): Notification = builder.build()

	companion object {

		const val NOTIFICATION_ID = 201
		const val CHANNEL_ID = "download"

		private const val PROGRESS_STEP = 20
	}
}