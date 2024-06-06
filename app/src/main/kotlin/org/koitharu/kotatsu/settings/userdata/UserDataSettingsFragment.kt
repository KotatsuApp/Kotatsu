package org.koitharu.kotatsu.settings.userdata

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.postDelayed
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import androidx.preference.forEach
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ScreenshotsPolicy
import org.koitharu.kotatsu.core.prefs.SearchSuggestionType
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.ui.util.ActivityRecreationHandle
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.setDefaultValueCompat
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.names
import org.koitharu.kotatsu.settings.backup.BackupDialogFragment
import org.koitharu.kotatsu.settings.backup.RestoreDialogFragment
import org.koitharu.kotatsu.settings.protect.ProtectSetupActivity
import org.koitharu.kotatsu.settings.utils.MultiSummaryProvider
import javax.inject.Inject

@AndroidEntryPoint
class UserDataSettingsFragment : BasePreferenceFragment(R.string.data_and_privacy),
	SharedPreferences.OnSharedPreferenceChangeListener,
	ActivityResultCallback<Uri?> {

	@Inject
	lateinit var appShortcutManager: AppShortcutManager

	@Inject
	lateinit var activityRecreationHandle: ActivityRecreationHandle

	private val viewModel: UserDataSettingsViewModel by viewModels()

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
		findPreference<ListPreference>(AppSettings.KEY_SCREENSHOTS_POLICY)?.run {
			entryValues = ScreenshotsPolicy.entries.names()
			setDefaultValueCompat(ScreenshotsPolicy.ALLOW.name)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_PAGES_CACHE_CLEAR)?.bindBytesSizeSummary(checkNotNull(viewModel.cacheSizes[CacheDir.PAGES]))
		findPreference<Preference>(AppSettings.KEY_THUMBS_CACHE_CLEAR)?.bindBytesSizeSummary(checkNotNull(viewModel.cacheSizes[CacheDir.THUMBS]))
		findPreference<Preference>(AppSettings.KEY_HTTP_CACHE_CLEAR)?.bindBytesSizeSummary(viewModel.httpCacheSize)
		bindPeriodicalBackupSummary()
		findPreference<Preference>(AppSettings.KEY_SEARCH_HISTORY_CLEAR)?.let { pref ->
			viewModel.searchHistoryCount.observe(viewLifecycleOwner) {
				pref.summary = if (it < 0) {
					view.context.getString(R.string.loading_)
				} else {
					pref.context.resources.getQuantityString(R.plurals.items, it, it)
				}
			}
		}
		findPreference<Preference>(AppSettings.KEY_UPDATES_FEED_CLEAR)?.let { pref ->
			viewModel.feedItemsCount.observe(viewLifecycleOwner) {
				pref.summary = if (it < 0) {
					view.context.getString(R.string.loading_)
				} else {
					pref.context.resources.getQuantityString(R.plurals.items, it, it)
				}
			}
		}
		findPreference<StorageUsagePreference>("storage_usage")?.let { pref ->
			viewModel.storageUsage.observe(viewLifecycleOwner, pref)
		}
		findPreference<MultiSelectListPreference>(AppSettings.KEY_SEARCH_SUGGESTION_TYPES)?.let { pref ->
			pref.entryValues = SearchSuggestionType.entries.names()
			pref.entries = SearchSuggestionType.entries.map { pref.context.getString(it.titleResId) }.toTypedArray()
			pref.summaryProvider = MultiSummaryProvider(R.string.none)
			pref.values = settings.searchSuggestionTypes.mapToSet { it.name }
		}
		viewModel.loadingKeys.observe(viewLifecycleOwner) { keys ->
			preferenceScreen.forEach { pref ->
				pref.isEnabled = pref.key !in keys
			}
		}
		viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(listView, this))
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(listView))
		viewModel.onChaptersCleanedUp.observeEvent(viewLifecycleOwner, ::onChaptersCleanedUp)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_PAGES_CACHE_CLEAR -> {
				viewModel.clearCache(preference.key, CacheDir.PAGES)
				true
			}

			AppSettings.KEY_THUMBS_CACHE_CLEAR -> {
				viewModel.clearCache(preference.key, CacheDir.THUMBS)
				true
			}

			AppSettings.KEY_COOKIES_CLEAR -> {
				clearCookies()
				true
			}

			AppSettings.KEY_SEARCH_HISTORY_CLEAR -> {
				clearSearchHistory()
				true
			}

			AppSettings.KEY_HTTP_CACHE_CLEAR -> {
				viewModel.clearHttpCache()
				true
			}

			AppSettings.KEY_CHAPTERS_CLEAR -> {
				cleanupChapters()
				true
			}

			AppSettings.KEY_UPDATES_FEED_CLEAR -> {
				viewModel.clearUpdatesFeed()
				true
			}

			AppSettings.KEY_BACKUP -> {
				BackupDialogFragment.show(childFragmentManager)
				true
			}

			AppSettings.KEY_RESTORE -> {
				if (!backupSelectCall.tryLaunch(arrayOf("*/*"))) {
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

			AppSettings.KEY_THEME -> {
				AppCompatDelegate.setDefaultNightMode(settings.theme)
			}

			AppSettings.KEY_COLOR_THEME,
			AppSettings.KEY_THEME_AMOLED -> {
				postRestart()
			}

			AppSettings.KEY_APP_LOCALE -> {
				AppCompatDelegate.setApplicationLocales(settings.appLocales)
			}
		}
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			RestoreDialogFragment.show(childFragmentManager, result)
		}
	}

	private fun onChaptersCleanedUp(result: Pair<Int, Long>) {
		val c = context ?: return
		val text = if (result.first == 0 && result.second == 0L) {
			c.getString(R.string.no_chapters_deleted)
		} else {
			c.getString(
				R.string.chapters_deleted_pattern,
				c.resources.getQuantityString(R.plurals.chapters, result.first, result.first),
				FileSize.BYTES.format(c, result.second),
			)
		}
		Snackbar.make(listView, text, Snackbar.LENGTH_SHORT).show()
	}


	private fun Preference.bindBytesSizeSummary(stateFlow: StateFlow<Long>) {
		stateFlow.observe(viewLifecycleOwner) { size ->
			summary = if (size < 0) {
				context.getString(R.string.computing_)
			} else {
				FileSize.BYTES.format(context, size)
			}
		}
	}

	private fun bindPeriodicalBackupSummary() {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_ENABLED) ?: return
		val entries = resources.getStringArray(R.array.backup_frequency)
		val entryValues = resources.getStringArray(R.array.values_backup_frequency)
		viewModel.periodicalBackupFrequency.observe(viewLifecycleOwner) { freq ->
			preference.summary = if (freq == 0L) {
				getString(R.string.disabled)
			} else {
				val index = entryValues.indexOf(freq.toString())
				entries.getOrNull(index)
			}
		}
	}

	private fun clearSearchHistory() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.clear_search_history)
			.setMessage(R.string.text_clear_search_history_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clearSearchHistory()
			}.show()
	}

	private fun clearCookies() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.clear_cookies)
			.setMessage(R.string.text_clear_cookies_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clearCookies()
			}.show()
	}

	private fun cleanupChapters() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.delete_read_chapters)
			.setMessage(R.string.delete_read_chapters_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.delete) { _, _ ->
				viewModel.cleanupChapters()
			}.show()
	}

	private fun postRestart() {
		view?.postDelayed(400) {
			activityRecreationHandle.recreateAll()
		}
	}

}
