package org.koitharu.kotatsu.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.local.Cache
import org.koitharu.kotatsu.domain.tracking.TrackingRepository
import org.koitharu.kotatsu.ui.base.BasePreferenceFragment
import org.koitharu.kotatsu.ui.search.MangaSuggestionsProvider
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.FileSizeUtils
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

class HistorySettingsFragment : BasePreferenceFragment(R.string.history_and_cache) {

	private val trackerRepo by inject<TrackingRepository>()

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_history)
		findPreference<Preference>(R.string.key_pages_cache_clear)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val size = withContext(Dispatchers.IO) {
					CacheUtils.computeCacheSize(pref.context, Cache.PAGES.dir)
				}
				pref.summary = FileSizeUtils.formatBytes(pref.context, size)
			}
		}
		findPreference<Preference>(R.string.key_thumbs_cache_clear)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val size = withContext(Dispatchers.IO) {
					CacheUtils.computeCacheSize(pref.context, Cache.THUMBS.dir)
				}
				pref.summary = FileSizeUtils.formatBytes(pref.context, size)
			}
		}
		findPreference<Preference>(R.string.key_search_history_clear)?.let { p ->
			val items = MangaSuggestionsProvider.getItemsCount(p.context)
			p.summary = p.context.resources.getQuantityString(R.plurals.items, items, items)
		}
		findPreference<Preference>(R.string.key_updates_feed_clear)?.let { p ->
			viewLifecycleScope.launchWhenResumed {
				val items = trackerRepo.count()
				p.summary = p.context.resources.getQuantityString(R.plurals.items, items, items)
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			getString(R.string.key_pages_cache_clear) -> {
				clearCache(preference, Cache.PAGES)
				true
			}
			getString(R.string.key_thumbs_cache_clear) -> {
				clearCache(preference, Cache.THUMBS)
				true
			}
			getString(R.string.key_search_history_clear) -> {
				MangaSuggestionsProvider.clearHistory(preference.context)
				preference.summary = preference.context.resources
					.getQuantityString(R.plurals.items, 0, 0)
				Snackbar.make(
					view ?: return true,
					R.string.search_history_cleared,
					Snackbar.LENGTH_SHORT
				).show()
				true
			}
			getString(R.string.key_updates_feed_clear) -> {
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
}