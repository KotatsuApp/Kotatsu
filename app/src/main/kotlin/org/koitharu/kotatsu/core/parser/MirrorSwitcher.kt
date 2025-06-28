package org.koitharu.kotatsu.core.parser

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.util.EnumSet
import javax.inject.Inject

class MirrorSwitcher @Inject constructor(
	private val settings: AppSettings,
	@MangaHttpClient private val okHttpClient: OkHttpClient,
) {

	private val blacklist = EnumSet.noneOf(MangaParserSource::class.java)
	private val mutex: Mutex = Mutex()

	val isEnabled: Boolean
		get() = settings.isMirrorSwitchingEnabled

	suspend fun <T : Any> trySwitchMirror(repository: ParserMangaRepository, loader: suspend () -> T?): T? {
		val source = repository.source
		if (!isEnabled || source in blacklist) {
			return null
		}
		val availableMirrors = repository.domains
		val currentHost = repository.domain
		if (availableMirrors.size <= 1 || currentHost !in availableMirrors) {
			return null
		}
		mutex.withLock {
			if (source in blacklist) {
				return null
			}
			logd { "Looking for mirrors for ${source}..." }
			findRedirect(repository)?.let { mirror ->
				repository.domain = mirror
				runCatchingCancellable {
					loader()?.takeIfValid()
				}.getOrNull()?.let {
					logd { "Found redirect for $source: $mirror" }
					return it
				}
			}
			for (mirror in availableMirrors) {
				repository.domain = mirror
				runCatchingCancellable {
					loader()?.takeIfValid()
				}.getOrNull()?.let {
					logd { "Found mirror for $source: $mirror" }
					return it
				}
			}
			repository.domain = currentHost // rollback
			blacklist.add(source)
			logd { "$source blacklisted" }
			return null
		}
	}

	suspend fun findRedirect(repository: ParserMangaRepository): String? {
		if (!isEnabled) {
			return null
		}
		val currentHost = repository.domain
		val newHost = okHttpClient.newCall(
			Request.Builder()
				.url("https://$currentHost")
				.head()
				.build(),
		).await().use {
			if (it.isSuccessful) {
				it.request.url.host
			} else {
				null
			}
		}
		return if (newHost != currentHost) {
			newHost
		} else {
			null
		}
	}

	private fun <T : Any> T.takeIfValid() = takeIf {
		when (it) {
			is Collection<*> -> it.isNotEmpty()
			else -> true
		}
	}

	private companion object {

		const val TAG = "MirrorSwitcher"

		inline fun logd(message: () -> String) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, message())
			}
		}
	}
}
