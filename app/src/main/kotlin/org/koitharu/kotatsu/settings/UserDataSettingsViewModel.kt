package org.koitharu.kotatsu.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import okhttp3.Cache
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import java.util.EnumMap
import javax.inject.Inject

@HiltViewModel
class UserDataSettingsViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val httpCache: Cache,
	private val searchRepository: MangaSearchRepository,
	private val trackingRepository: TrackingRepository,
	private val cookieJar: MutableCookieJar,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val loadingKeys = MutableStateFlow(emptySet<String>())

	val searchHistoryCount = MutableStateFlow(-1)
	val feedItemsCount = MutableStateFlow(-1)
	val httpCacheSize = MutableStateFlow(-1L)
	val cacheSizes = EnumMap<CacheDir, MutableStateFlow<Long>>(CacheDir::class.java)

	init {
		CacheDir.values().forEach {
			cacheSizes[it] = MutableStateFlow(-1L)
		}
		launchJob(Dispatchers.Default) {
			searchHistoryCount.value = searchRepository.getSearchHistoryCount()
		}
		launchJob(Dispatchers.Default) {
			feedItemsCount.value = trackingRepository.getLogsCount()
		}
		CacheDir.values().forEach { cache ->
			launchJob(Dispatchers.Default) {
				checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
			}
		}
		launchJob(Dispatchers.Default) {
			httpCacheSize.value = runInterruptible { httpCache.size() }
		}
	}

	fun clearCache(key: String, cache: CacheDir) {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + key }
				storageManager.clearCache(cache)
				checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
			} finally {
				loadingKeys.update { it - key }
			}
		}
	}

	fun clearHttpCache() {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + AppSettings.KEY_HTTP_CACHE_CLEAR }
				val size = runInterruptible(Dispatchers.IO) {
					httpCache.evictAll()
					httpCache.size()
				}
				httpCacheSize.value = size
			} finally {
				loadingKeys.update { it - AppSettings.KEY_HTTP_CACHE_CLEAR }
			}
		}
	}

	fun clearSearchHistory() {
		launchJob(Dispatchers.Default) {
			searchRepository.clearSearchHistory()
			searchHistoryCount.value = searchRepository.getSearchHistoryCount()
			onActionDone.call(ReversibleAction(R.string.search_history_cleared, null))
		}
	}

	fun clearCookies() {
		launchJob {
			cookieJar.clear()
			onActionDone.call(ReversibleAction(R.string.cookies_cleared, null))
		}
	}

	fun clearUpdatesFeed() {
		launchJob(Dispatchers.Default) {
			trackingRepository.clearLogs()
			feedItemsCount.value = trackingRepository.getLogsCount()
			onActionDone.call(ReversibleAction(R.string.updates_feed_cleared, null))
		}
	}
}
