package org.koitharu.kotatsu.reader.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.entities.presence.Activity
import com.my.kizzyrpc.entities.presence.Assets
import com.my.kizzyrpc.entities.presence.Timestamps
import com.my.kizzyrpc.entities.presence.Metadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscordRPCService : Service() {

	private val scope = CoroutineScope(SupervisorJob())
	private var rpc: KizzyRPC? = null

	companion object {
		const val CHANNEL_ID = "KotatsuRPC"
		const val START_RPC_ACTION = "START_RPC_ACTION"
		const val UPDATE_RPC_ACTION = "UPDATE_RPC_ACTION"
		const val STOP_RPC_ACTION = "STOP_RPC_ACTION"
		const val EXTRA_MANGA_TITLE = "manga_title"
		const val EXTRA_CHAPTER_NUMBER = "chapter_number"
		const val EXTRA_CURRENT_PAGE = "current_page"
		const val EXTRA_TOTAL_PAGES = "total_pages"
		const val EXTRA_MANGA_LINK = "manga_link"
		const val EXTRA_MANGA_APP_URL = "manga_app_url"
		var token: String? = null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		token = intent?.getStringExtra("TOKEN")
		rpc = token?.let { KizzyRPC(it) }

		when(intent?.action) {
			START_RPC_ACTION -> {
				updateRpcActivity(intent)
			}
			UPDATE_RPC_ACTION -> {
				updateRpcActivity(intent)
			}
			STOP_RPC_ACTION -> {
				clearRpcActivity()
			}
		}
		return START_STICKY
	}

	private fun updateRpcActivity(intent: Intent) {
		val mangaTitle = intent.getStringExtra(EXTRA_MANGA_TITLE) ?: return
		val chapterNumber = intent.getIntExtra(EXTRA_CHAPTER_NUMBER, 1)
		val currentPage = intent.getIntExtra(EXTRA_CURRENT_PAGE, 1)
		val totalPages = intent.getIntExtra(EXTRA_TOTAL_PAGES, 0)
		val mangaLink = intent.getStringExtra(EXTRA_MANGA_LINK)
		val mangaAppUrl = intent.getStringExtra(EXTRA_MANGA_APP_URL)

		scope.launch {
			rpc?.updateRPC(
				activity = Activity(
					applicationId = "1395464028611940393",
					name = "Kotatsu",
					details = mangaTitle,
					state = "Chapter: $chapterNumber - Page: $currentPage/$totalPages",
					type = 0,
					timestamps = Timestamps(
						start = System.currentTimeMillis()
					),
					assets = Assets(
						largeImage = "mp:attachments/1396092865544716390/1396123149921419465/Kotatsu.png?ex=687d9941&is=687c47c1&hm=61da2b66445adaea18ad16cc2c7f829d1c97f0622beec332f123a56f4d294820&=&format=webp&quality=lossless&width=256&height=256",
						largeText = "Reading manga on Kotatsu - A manga reader app",
						smallText = "Reading $mangaTitle",
						smallImage = "mp:attachments/1282576939831529473/1395712714415800392/button.png?ex=687b7242&is=687a20c2&hm=828ad97537c94128504402b43512523fe30801d534a48258f80c6fd29fda67c2&=&format=webp&quality=lossless",
					),
					buttons = listOf("Link to Kotatsu", "Link to manga source"),
					metadata = Metadata(
						listOf(
							mangaAppUrl,
							mangaLink,
						)
					)
				),
				status = "online",
				since = System.currentTimeMillis()
			)
		}
	}

	private fun clearRpcActivity() {
		scope.launch {
			rpc?.closeRPC()
		}
	}

	override fun onDestroy() {
		scope.launch {
			rpc?.closeRPC()
		}
		super.onDestroy()
	}

	override fun onBind(intent: Intent?): IBinder? = null
}
