package org.koitharu.kotatsu.settings.userdata

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import okhttp3.Cache
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.firstNotNull
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.domain.DeleteReadChaptersUseCase
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
	private val settings: AppSettings,
	private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val loadingKeys = MutableStateFlow(emptySet<String>())

	val searchHistoryCount = MutableStateFlow(-1)
	val feedItemsCount = MutableStateFlow(-1)
	val httpCacheSize = MutableStateFlow(-1L)
	val cacheSizes = EnumMap<CacheDir, MutableStateFlow<Long>>(CacheDir::class.java)
	val storageUsage = MutableStateFlow<StorageUsage?>(null)

	val onChaptersCleanedUp = MutableEventFlow<Pair<Int, Long>>()

	val periodicalBackupFrequency = settings.observeAsFlow(
		key = AppSettings.KEY_BACKUP_PERIODICAL_ENABLED,
		valueProducer = { isPeriodicalBackupEnabled },
	).flatMapLatest { isEnabled ->
		if (isEnabled) {
			settings.observeAsFlow(
				key = AppSettings.KEY_BACKUP_PERIODICAL_FREQUENCY,
				valueProducer = { periodicalBackupFrequency },
			)
		} else {
			flowOf(0)
		}
	}

	private var storageUsageJob: Job? = null

	init {
		CacheDir.entries.forEach {
			cacheSizes[it] = MutableStateFlow(-1L)
		}
		launchJob(Dispatchers.Default) {
			searchHistoryCount.value = searchRepository.getSearchHistoryCount()
		}
		launchJob(Dispatchers.Default) {
			feedItemsCount.value = trackingRepository.getLogsCount()
		}
		CacheDir.entries.forEach { cache ->
			launchJob(Dispatchers.Default) {
				checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
			}
		}
		launchJob(Dispatchers.Default) {
			httpCacheSize.value = runInterruptible { httpCache.size() }
		}
		loadStorageUsage()
	}

	fun clearCache(key: String, cache: CacheDir) {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + key }
				storageManager.clearCache(cache)
				checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
				loadStorageUsage()
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
				loadStorageUsage()
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

	fun cleanupChapters() {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + AppSettings.KEY_CHAPTERS_CLEAR }
				val oldSize = storageUsage.firstNotNull().savedManga.bytes
				val chaptersCount = deleteReadChaptersUseCase.invoke()
				loadStorageUsage().join()
				val newSize = storageUsage.firstNotNull().savedManga.bytes
				onChaptersCleanedUp.call(chaptersCount to oldSize - newSize)
			} finally {
				loadingKeys.update { it - AppSettings.KEY_CHAPTERS_CLEAR }
			}
		}
	}

	private fun loadStorageUsage(): Job {
		val prevJob = storageUsageJob
		return launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val pagesCacheSize = storageManager.computeCacheSize(CacheDir.PAGES)
			val otherCacheSize = storageManager.computeCacheSize() - pagesCacheSize
			val storageSize = storageManager.computeStorageSize()
			val availableSpace = storageManager.computeAvailableSize()
			val totalBytes = pagesCacheSize + otherCacheSize + storageSize + availableSpace
			storageUsage.value = StorageUsage(
				savedManga = StorageUsage.Item(
					bytes = storageSize,
					percent = (storageSize.toDouble() / totalBytes).toFloat(),
				),
				pagesCache = StorageUsage.Item(
					bytes = pagesCacheSize,
					percent = (pagesCacheSize.toDouble() / totalBytes).toFloat(),
				),
				otherCache = StorageUsage.Item(
					bytes = otherCacheSize,
					percent = (otherCacheSize.toDouble() / totalBytes).toFloat(),
				),
				available = StorageUsage.Item(
					bytes = availableSpace,
					percent = (availableSpace.toDouble() / totalBytes).toFloat(),
				),
			)
		}.also {
			storageUsageJob = it
		}
	}
}
