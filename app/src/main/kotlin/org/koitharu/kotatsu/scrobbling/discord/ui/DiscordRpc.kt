package org.koitharu.kotatsu.scrobbling.discord.ui

import android.content.Context
import android.os.SystemClock
import androidx.annotation.AnyThread
import androidx.collection.ArrayMap
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.entities.presence.Activity
import com.my.kizzyrpc.entities.presence.Assets
import com.my.kizzyrpc.entities.presence.Metadata
import com.my.kizzyrpc.entities.presence.Timestamps
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import okio.utf8Size
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.LocalizedAppContext
import org.koitharu.kotatsu.core.model.appUrl
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.lifecycleScope
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.scrobbling.discord.data.DiscordRepository
import java.util.Collections
import javax.inject.Inject

private const val STATUS_ONLINE = "online"
private const val STATUS_IDLE = "idle"
private const val BUTTON_TEXT_LIMIT = 32
private const val DEBOUNCE_TIMEOUT = 16_000L // 16 sec

@ViewModelScoped
class DiscordRpc @Inject constructor(
	@LocalizedAppContext private val context: Context,
	private val settings: AppSettings,
	private val repository: DiscordRepository,
	lifecycle: ViewModelLifecycle,
) : RetainedLifecycle.OnClearedListener {

	private val coroutineScope = lifecycle.lifecycleScope + Dispatchers.Default
	private val appId = context.getString(R.string.discord_app_id)
	private val appName = context.getString(R.string.app_name)
	private val appIcon = context.getString(R.string.app_icon_url)
	private val mpCache = Collections.synchronizedMap(ArrayMap<String, String>())
	private var lastUpdate = 0L

	private var rpc: KizzyRPC? = null

	private var rpcUpdateJob: Job? = null

	@Volatile
	private var lastActivity: Activity? = null

	init {
		lifecycle.addOnClearedListener(this)
	}

	override fun onCleared() {
		clearRpc()
	}

	fun clearRpc() = synchronized(this) {
		rpc?.closeRPC()
		rpc = null
		lastUpdate = 0L
	}

	fun setIdle() {
		lastActivity?.let { activity ->
			getRpc()?.updateRpcAsync(activity, idle = true)
		}
	}

	@AnyThread
	fun updateRpc(manga: Manga, state: ReaderUiState) {
		getRpc()?.run {
			if (settings.isDiscordRpcSkipNsfw && manga.isNsfw()) {
				clearRpc()
				return
			}
			updateRpcAsync(
				activity = Activity(
					applicationId = appId,
					name = appName,
					details = manga.title,
					state = context.getString(R.string.chapter_d_of_d, state.chapterNumber, state.chaptersTotal),
					type = 3,
					timestamps = Timestamps(
						start = lastActivity?.timestamps?.start ?: System.currentTimeMillis(),
					),
					assets = Assets(
						largeImage = manga.coverUrl,
						largeText = context.getString(R.string.reading_s, manga.title),
						smallText = context.getString(R.string.discord_rpc_description),
						smallImage = appIcon,
					),
					buttons = listOf(
						context.getString(R.string.read_on_s, appName),
						context.getString(R.string.read_on_s, manga.source.getTitle(context)),
					),
					metadata = Metadata(listOf(manga.appUrl.toString(), manga.publicUrl)),
				),
				idle = false,
			)
		}
	}

	private fun KizzyRPC.updateRpcAsync(activity: Activity, idle: Boolean) {
		val prevJob = rpcUpdateJob
		rpcUpdateJob = coroutineScope.launch {
			prevJob?.cancelAndJoin()
			val debounceTime = lastUpdate + DEBOUNCE_TIMEOUT - SystemClock.elapsedRealtime()
			if (debounceTime > 0) {
				delay(debounceTime)
			}
			val hideButtons = activity.buttons?.any { it != null && it.utf8Size() > BUTTON_TEXT_LIMIT } ?: false
			val mappedActivity = activity.copy(
				assets = activity.assets?.let {
					it.copy(
						largeImage = it.largeImage?.toMediaProxyUrl(),
						smallImage = it.smallImage?.toMediaProxyUrl(),
					)
				},
				buttons = activity.buttons.takeUnless { hideButtons },
				metadata = activity.metadata.takeUnless { hideButtons },
			)
			lastActivity = mappedActivity
			updateRPC(
				activity = mappedActivity,
				status = if (idle) STATUS_IDLE else STATUS_ONLINE,
				since = activity.timestamps?.start ?: System.currentTimeMillis(),
			)
			lastUpdate = SystemClock.elapsedRealtime()
		}
	}

	suspend fun String.toMediaProxyUrl(): String? {
		if (repository.isMediaProxyUrl(this)) {
			return this
		}
		mpCache[this]?.let {
			return it
		}
		return runCatchingCancellable {
			repository.getMediaProxyUrl(this)
		}.onSuccess { url ->
			mpCache[this] = url
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	private fun getRpc(): KizzyRPC? {
		rpc?.let {
			return it
		}
		return synchronized(this) {
			rpc?.let {
				return@synchronized it
			}
			if (settings.isDiscordRpcEnabled) {
				settings.discordToken?.let { KizzyRPC(it) }
			} else {
				null
			}.also {
				rpc = it
			}
		}
	}
}
