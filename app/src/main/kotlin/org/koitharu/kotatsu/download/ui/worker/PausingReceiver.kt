package org.koitharu.kotatsu.download.ui.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.PatternMatcher
import androidx.core.app.PendingIntentCompat
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
			ACTION_PAUSE -> pausingHandle.pause()
		}
	}

	companion object {

		private const val ACTION_PAUSE = "org.koitharu.kotatsu.download.PAUSE"
		private const val ACTION_RESUME = "org.koitharu.kotatsu.download.RESUME"
		private const val EXTRA_UUID = "uuid"
		private const val SCHEME = "workuid"

		fun createIntentFilter(id: UUID) = IntentFilter().apply {
			addAction(ACTION_PAUSE)
			addAction(ACTION_RESUME)
			addDataScheme(SCHEME)
			addDataPath(id.toString(), PatternMatcher.PATTERN_SIMPLE_GLOB)
		}

		fun getPauseIntent(id: UUID) = Intent(ACTION_PAUSE)
			.setData(Uri.parse("$SCHEME://$id"))
			.putExtra(EXTRA_UUID, id.toString())

		fun getResumeIntent(id: UUID) = Intent(ACTION_RESUME)
			.setData(Uri.parse("$SCHEME://$id"))
			.putExtra(EXTRA_UUID, id.toString())

		fun createPausePendingIntent(context: Context, id: UUID) = PendingIntentCompat.getBroadcast(
			context,
			0,
			getPauseIntent(id),
			0,
			false,
		)

		fun createResumePendingIntent(context: Context, id: UUID) = PendingIntentCompat.getBroadcast(
			context,
			0,
			getResumeIntent(id),
			0,
			false,
		)
	}
}
