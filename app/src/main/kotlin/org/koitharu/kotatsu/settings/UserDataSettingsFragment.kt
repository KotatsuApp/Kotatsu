package org.koitharu.kotatsu.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okhttp3.Cache
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.awaitStateAtLeast
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.settings.backup.BackupDialogFragment
import org.koitharu.kotatsu.settings.backup.RestoreDialogFragment
import org.koitharu.kotatsu.settings.protect.ProtectSetupActivity
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import javax.inject.Inject

@AndroidEntryPoint
class UserDataSettingsFragment : BasePreferenceFragment(R.string.data_and_privacy),
	SharedPreferences.OnSharedPreferenceChangeListener,
	ActivityResultCallback<Uri?> {

	@Inject
	lateinit var trackerRepo: TrackingRepository

	@Inject
	lateinit var searchRepository: MangaSearchRepository

	@Inject
	lateinit var storageManager: LocalStorageManager

	@Inject
	lateinit var cookieJar: MutableCookieJar

	@Inject
	lateinit var cache: Cache

	@Inject
	lateinit var appShortcutManager: AppShortcutManager

	private val backupSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
		this,
	)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_user_data)
		findPreference<Preference>(AppSettings.KEY_SHORTCUTS)?.isVisible =
			appShortcutManager.isDynamicShortcutsAvailable()
		findPreference<TwoStatePreference>(AppSettings.KEY_PROTECT_APP)
			?.isChecked = !settings.appPassword.isNullOrEmpty()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_PAGES_CACHE_CLEAR)?.bindSummaryToCacheSize(CacheDir.PAGES)
		findPreference<Preference>(AppSettings.KEY_THUMBS_CACHE_CLEAR)?.bindSummaryToCacheSize(CacheDir.THUMBS)
		findPreference<Preference>(AppSettings.KEY_HTTP_CACHE_CLEAR)?.bindSummaryToHttpCacheSize()
		findPreference<Preference>(AppSettings.KEY_SEARCH_HISTORY_CLEAR)?.let { pref ->
			viewLifecycleScope.launch {
				lifecycle.awaitStateAtLeast(Lifecycle.State.RESUMED)
				val items = searchRepository.getSearchHistoryCount()
				pref.summary = pref.context.resources.getQuantityString(R.plurals.items, items, items)
			}
		}
		findPreference<Preference>(AppSettings.KEY_UPDATES_FEED_CLEAR)?.let { pref ->
			viewLifecycleScope.launch {
				lifecycle.awaitStateAtLeast(Lifecycle.State.RESUMED)
				val items = trackerRepo.getLogsCount()
				pref.summary = pref.context.resources.getQuantityString(R.plurals.items, items, items)
			}
		}
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
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

			AppSettings.KEY_HTTP_CACHE_CLEAR -> {
				clearHttpCache()
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

			AppSettings.KEY_BACKUP -> {
				BackupDialogFragment.show(childFragmentManager)
				true
			}

			AppSettings.KEY_RESTORE -> {
				try {
					backupSelectCall.launch(arrayOf("*/*"))
				} catch (e: ActivityNotFoundException) {
					e.printStackTraceDebug()
					Snackbar.make(
						listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
					).show()
				}
				true
			}

			AppSettings.KEY_PROTECT_APP -> {
				val pref = (preference as? TwoStatePreference ?: return false)
				if (pref.isChecked) {
					pref.isChecked = false
					startActivity(Intent(preference.context, ProtectSetupActivity::class.java))
				} else {
					settings.appPassword = null
				}
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_APP_PASSWORD -> {
				findPreference<TwoStatePreference>(AppSettings.KEY_PROTECT_APP)
					?.isChecked = !settings.appPassword.isNullOrEmpty()
			}
		}
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			RestoreDialogFragment.show(childFragmentManager, result)
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

	private fun Preference.bindSummaryToHttpCacheSize() = viewLifecycleScope.launch {
		val size = runInterruptible(Dispatchers.IO) { cache.size() }
		summary = FileSize.BYTES.format(context, size)
	}

	private fun clearHttpCache() {
		val preference = findPreference<Preference>(AppSettings.KEY_HTTP_CACHE_CLEAR) ?: return
		val ctx = preference.context.applicationContext
		viewLifecycleScope.launch {
			try {
				preference.isEnabled = false
				val size = runInterruptible(Dispatchers.IO) {
					cache.evictAll()
					cache.size()
				}
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
}
