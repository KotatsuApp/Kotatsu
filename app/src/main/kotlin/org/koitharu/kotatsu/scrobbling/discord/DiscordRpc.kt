package org.koitharu.kotatsu.scrobbling.discord

import android.content.Context
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.entities.presence.Activity
import com.my.kizzyrpc.entities.presence.Assets
import com.my.kizzyrpc.entities.presence.Metadata
import com.my.kizzyrpc.entities.presence.Timestamps
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.LocalizedAppContext
import org.koitharu.kotatsu.core.model.appUrl
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.lifecycleScope
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import javax.inject.Inject

private const val STATUS_ONLINE = "online"
private const val STATUS_IDLE = "idle"

@ViewModelScoped
class DiscordRpc @Inject constructor(
	@LocalizedAppContext private val context: Context,
	private val settings: AppSettings,
	lifecycle: ViewModelLifecycle,
) : RetainedLifecycle.OnClearedListener {

	private val coroutineScope = lifecycle.lifecycleScope + Dispatchers.Default
	private val appId = context.getString(R.string.discord_app_id)
	private val appName = context.getString(R.string.app_name)
	private val rpc = if (settings.isDiscordRpcEnabled) {
		settings.discordToken?.let { KizzyRPC(it) }
	} else {
		null
	}

	private var lastActivity: Activity? = null

	init {
		lifecycle.addOnClearedListener(this)
	}

	override fun onCleared() {
		clearRpc()
	}

	fun clearRpc() {
		rpc?.closeRPC()
	}

	fun setIdle() {
		if (rpc != null) {
			lastActivity?.let { activity ->
				updateRpcAsync(activity, idle = true)
			}
		}
	}

	fun updateRpc(manga: Manga, state: ReaderUiState) {
		if (rpc != null) {
			updateRpcAsync(
				activity = Activity(
					applicationId = appId,
					name = appName,
					details = manga.title,
					state = context.getString(R.string.chapter_d_of_d, state.chapterNumber, state.chaptersTotal),
					type = 3,
					timestamps = Timestamps(
						start = System.currentTimeMillis(),
					),
					assets = Assets(
						largeImage = "mp:attachments/1396092865544716390/1396123149921419465/Kotatsu.png?ex=687d9941&is=687c47c1&hm=61da2b66445adaea18ad16cc2c7f829d1c97f0622beec332f123a56f4d294820&=&format=webp&quality=lossless&width=256&height=256",
						largeText = "Reading manga on Kotatsu - A manga reader app",
						smallText = "Reading: ${manga.title}",
						smallImage = "mp:attachments/1282576939831529473/1395712714415800392/button.png?ex=687b7242&is=687a20c2&hm=828ad97537c94128504402b43512523fe30801d534a48258f80c6fd29fda67c2&=&format=webp&quality=lossless",
					),
					buttons = listOf(
						context.getString(R.string.link_to_manga_in_app),
						context.getString(R.string.link_to_manga_on_s, manga.source.getTitle(context)),
					),
					metadata = Metadata(listOf(manga.appUrl.toString(), manga.publicUrl)),
				),
				idle = false,
			)
		}
	}

	private fun updateRpcAsync(activity: Activity, idle: Boolean) {
		val rpc = rpc ?: return
		lastActivity = activity
		coroutineScope.launch {
			rpc.updateRPC(
				activity = activity,
				status = if (idle) STATUS_IDLE else STATUS_ONLINE,
			)
		}
	}
}
