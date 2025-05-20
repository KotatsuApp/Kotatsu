package org.koitharu.kotatsu.download.ui.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import org.koitharu.kotatsu.core.util.ext.toUUIDOrNull
import java.util.UUID

class PausingReceiver(
	private val id: UUID,
	private val pausingHandle: PausingHandle,
) : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent?) {
		val uuid = intent?.getStringExtra(EXTRA_UUID)?.toUUIDOrNull()
		if (uuid != id) {
			return
		}
		when (intent.action) {
			ACTION_RESUME -> pausingHandle.resume()
			ACTION_SKIP -> pausingHandle.skip()
			ACTION_SKIP_ALL -> pausingHandle.skipAll()
			ACTION_PAUSE -> pausingHandle.pause()
		}
	}

	companion object {

		private const val ACTION_PAUSE = "org.koitharu.kotatsu.download.PAUSE"
		private const val ACTION_RESUME = "org.koitharu.kotatsu.download.RESUME"
		private const val ACTION_SKIP = "org.koitharu.kotatsu.download.SKIP"
		private const val ACTION_SKIP_ALL = "org.koitharu.kotatsu.download.SKIP_ALL"
		private const val EXTRA_UUID = "uuid"
		private const val SCHEME = "workuid"

		fun createIntentFilter(id: UUID) = IntentFilter().apply {
			addAction(ACTION_PAUSE)
			addAction(ACTION_RESUME)
			addAction(ACTION_SKIP)
			addAction(ACTION_SKIP_ALL)
			addDataScheme(SCHEME)
			addDataPath(id.toString(), PatternMatcher.PATTERN_LITERAL)
		}

		fun getPauseIntent(context: Context, id: UUID) = createIntent(context, id, ACTION_PAUSE)

		fun getResumeIntent(context: Context, id: UUID) = createIntent(context, id, ACTION_RESUME)

		fun getSkipIntent(context: Context, id: UUID) = createIntent(context, id, ACTION_SKIP)

		fun getSkipAllIntent(context: Context, id: UUID) = createIntent(context, id, ACTION_SKIP_ALL)

		fun createPausePendingIntent(context: Context, id: UUID) = PendingIntentCompat.getBroadcast(
			context,
			0,
			getPauseIntent(context, id),
			0,
			false,
		)

		fun createResumePendingIntent(context: Context, id: UUID) =
			PendingIntentCompat.getBroadcast(
				context,
				0,
				getResumeIntent(context, id),
				0,
				false,
			)

		fun createSkipPendingIntent(context: Context, id: UUID) =
			PendingIntentCompat.getBroadcast(
				context,
				0,
				getSkipIntent(context, id),
				0,
				false,
			)

		private fun createIntent(context: Context, id: UUID, action: String) = Intent(action)
			.setData("$SCHEME://$id".toUri())
			.setPackage(context.packageName)
			.putExtra(EXTRA_UUID, id.toString())
	}
}
