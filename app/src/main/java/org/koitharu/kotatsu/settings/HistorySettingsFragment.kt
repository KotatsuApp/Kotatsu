package org.koitharu.kotatsu.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.network.AndroidCookieJar
import org.koitharu.kotatsu.core.os.ShortcutsUpdater
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriRepository
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import javax.inject.Inject

@AndroidEntryPoint
class HistorySettingsFragment : BasePreferenceFragment(R.string.history_and_cache) {

	@Inject
	lateinit var trackerRepo: TrackingRepository

	@Inject
	lateinit var searchRepository: MangaSearchRepository

	@Inject
	lateinit var storageManager: LocalStorageManager

	@Inject
	lateinit var shikimoriRepository: ShikimoriRepository

	@Inject
	lateinit var cookieJar: AndroidCookieJar

	@Inject
	lateinit var shortcutsUpdater: ShortcutsUpdater

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_history)
		findPreference<Preference>(AppSettings.KEY_SHORTCUTS)?.isVisible =
			shortcutsUpdater.isDynamicShortcutsAvailable()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_PAGES_CACHE_CLEAR)?.bindSummaryToCacheSize(CacheDir.PAGES)
		findPreference<Preference>(AppSettings.KEY_THUMBS_CACHE_CLEAR)?.bindSummaryToCacheSize(CacheDir.THUMBS)
		findPreference<Preference>(AppSettings.KEY_SEARCH_HISTORY_CLEAR)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val items = searchRepository.getSearchHistoryCount()
				pref.summary =
					pref.context.resources.getQuantityString(R.plurals.items, items, items)
			}
		}
		findPreference<Preference>(AppSettings.KEY_UPDATES_FEED_CLEAR)?.let { pref ->
			viewLifecycleScope.launchWhenResumed {
				val items = trackerRepo.getLogsCount()
				pref.summary =
					pref.context.resources.getQuantityString(R.plurals.items, items, items)
			}
		}
	}

	override fun onResume() {
		super.onResume()
		bindShikimoriSummary()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_PAGES_CACHE_CLEAR -> {
				clearCache(preference, CacheDir.PAGES)
				true
			}

			AppSettings.KEY_THUMBS_CACHE_CLEAR -> {
				clearCache(preference, CacheDir.THUMBS)
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
						Snackbar.LENGTH_SHORT,
					).show()
				}
				true
			}

			AppSettings.KEY_SHIKIMORI -> {
				if (!shikimoriRepository.isAuthorized) {
					launchShikimoriAuth()
					true
				} else {
					super.onPreferenceTreeClick(preference)
				}
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun clearCache(preference: Preference, cache: CacheDir) {
		val ctx = preference.context.applicationContext
		viewLifecycleScope.launch {
			try {
				preference.isEnabled = false
				storageManager.clearCache(cache)
				val size = storageManager.computeCacheSize(cache)
				preference.summary = FileSize.BYTES.format(ctx, size)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				preference.summary = e.getDisplayMessage(ctx.resources)
			} finally {
				preference.isEnabled = true
			}
		}
	}

	private fun Preference.bindSummaryToCacheSize(dir: CacheDir) = viewLifecycleScope.launch {
		val size = storageManager.computeCacheSize(dir)
		summary = FileSize.BYTES.format(context, size)
	}

	private fun clearSearchHistory(preference: Preference) {
		MaterialAlertDialogBuilder(context ?: return)
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
						Snackbar.LENGTH_SHORT,
					).show()
				}
			}.show()
	}

	private fun clearCookies() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.clear_cookies)
			.setMessage(R.string.text_clear_cookies_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewLifecycleScope.launch {
					cookieJar.clear()
					Snackbar.make(
						listView ?: return@launch,
						R.string.cookies_cleared,
						Snackbar.LENGTH_SHORT,
					).show()
				}
			}.show()
	}

	private fun bindShikimoriSummary() {
		findPreference<Preference>(AppSettings.KEY_SHIKIMORI)?.summary = if (shikimoriRepository.isAuthorized) {
			getString(R.string.logged_in_as, shikimoriRepository.getCachedUser()?.nickname)
		} else {
			getString(R.string.disabled)
		}
	}

	private fun launchShikimoriAuth() {
		runCatching {
			val intent = Intent(Intent.ACTION_VIEW)
			intent.data = Uri.parse(shikimoriRepository.oauthUrl)
			startActivity(intent)
		}.onFailure {
			Snackbar.make(listView, it.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
		}
	}
}
