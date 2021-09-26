package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.network.AndroidCookieJar
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.Cache
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.FileSizeUtils
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

class HistorySettingsFragment : BasePreferenceFragment(R.string.history_and_cache) {

	private val trackerRepo by inject<TrackingRepository>(mode = LazyThreadSafetyMode.NONE)
	private val searchRepository by inject<MangaSearchRepository>(mode = LazyThreadSafetyMode.NONE)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_history)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_PAGES_CACHE_CLEAR)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val size = withContext(Dispatchers.IO) {
					CacheUtils.computeCacheSize(pref.context, Cache.PAGES.dir)
				}
				pref.summary = FileSizeUtils.formatBytes(pref.context, size)
			}
		}
		findPreference<Preference>(AppSettings.KEY_THUMBS_CACHE_CLEAR)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val size = withContext(Dispatchers.IO) {
					CacheUtils.computeCacheSize(pref.context, Cache.THUMBS.dir)
				}
				pref.summary = FileSizeUtils.formatBytes(pref.context, size)
			}
		}
		findPreference<Preference>(AppSettings.KEY_SEARCH_HISTORY_CLEAR)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val items = searchRepository.getSearchHistoryCount()
				pref.summary =
					pref.context.resources.getQuantityString(R.plurals.items, items, items)
			}
		}
		findPreference<Preference>(AppSettings.KEY_UPDATES_FEED_CLEAR)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val items = trackerRepo.count()
				pref.summary =
					pref.context.resources.getQuantityString(R.plurals.items, items, items)
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_PAGES_CACHE_CLEAR -> {
				clearCache(preference, Cache.PAGES)
				true
			}
			AppSettings.KEY_THUMBS_CACHE_CLEAR -> {
				clearCache(preference, Cache.THUMBS)
				true
			}
			AppSettings.KEY_COOKIES_CLEAR -> {
				clearCookies()
				true
			}
			AppSettings.KEY_SEARCH_HISTORY_CLEAR -> {
				clearSearchHistory(preference)
				true
			}
			AppSettings.KEY_UPDATES_FEED_CLEAR -> {
				viewLifecycleScope.launch {
					trackerRepo.clearLogs()
					preference.summary = preference.context.resources
						.getQuantityString(R.plurals.items, 0, 0)
					Snackbar.make(
						view ?: return@launch,
						R.string.updates_feed_cleared,
						Snackbar.LENGTH_SHORT
					).show()
				}
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun clearCache(preference: Preference, cache: Cache) {
		val ctx = preference.context.applicationContext
		viewLifecycleScope.launch {
			try {
				preference.isEnabled = false
				val size = withContext(Dispatchers.IO) {
					CacheUtils.clearCache(ctx, cache.dir)
					CacheUtils.computeCacheSize(ctx, cache.dir)
				}
				preference.summary = FileSizeUtils.formatBytes(ctx, size)
			} catch (e: Exception) {
				preference.summary = e.getDisplayMessage(ctx.resources)
			} finally {
				preference.isEnabled = true
			}
		}
	}

	private fun clearSearchHistory(preference: Preference) {
		AlertDialog.Builder(context ?: return)
			.setTitle(R.string.clear_search_history)
			.setMessage(R.string.text_clear_search_history_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewLifecycleScope.launch {
					searchRepository.clearSearchHistory()
					preference.summary = preference.context.resources
						.getQuantityString(R.plurals.items, 0, 0)
					Snackbar.make(
						view ?: return@launch,
						R.string.search_history_cleared,
						Snackbar.LENGTH_SHORT
					).show()
				}
			}.show()
	}

	private fun clearCookies() {
		AlertDialog.Builder(context ?: return)
			.setTitle(R.string.clear_cookies)
			.setMessage(R.string.text_clear_cookies_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewLifecycleScope.launch {
					val cookieJar = get<AndroidCookieJar>()
					cookieJar.clear()
					Snackbar.make(
						listView ?: return@launch,
						R.string.cookies_cleared,
						Snackbar.LENGTH_SHORT
					).show()
				}
			}.show()
	}
}