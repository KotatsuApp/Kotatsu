package org.koitharu.kotatsu.settings

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.base.ui.dialog.StorageSelectDialog
import org.koitharu.kotatsu.core.network.DoHProvider
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.parsers.util.names
import org.koitharu.kotatsu.settings.utils.SliderPreference
import org.koitharu.kotatsu.utils.ext.getStorageName
import org.koitharu.kotatsu.utils.ext.setDefaultValueCompat
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import java.io.File

class ContentSettingsFragment :
	BasePreferenceFragment(R.string.content),
	SharedPreferences.OnSharedPreferenceChangeListener,
	StorageSelectDialog.OnStorageSelectListener {

	private val storageManager by inject<LocalStorageManager>()

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_content)

		findPreference<Preference>(AppSettings.KEY_SUGGESTIONS)?.setSummary(
			if (settings.isSuggestionsEnabled) R.string.enabled else R.string.disabled
		)
		findPreference<SliderPreference>(AppSettings.KEY_DOWNLOADS_PARALLELISM)?.run {
			summary = value.toString()
			setOnPreferenceChangeListener { preference, newValue ->
				preference.summary = newValue.toString()
				true
			}
		}
		findPreference<ListPreference>(AppSettings.KEY_DOH)?.run {
			entryValues = arrayOf(
				DoHProvider.NONE,
				DoHProvider.GOOGLE,
				DoHProvider.CLOUDFLARE,
				DoHProvider.ADGUARD,
			).names()
			setDefaultValueCompat(DoHProvider.NONE.name)
		}
		bindRemoteSourcesSummary()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_LOCAL_STORAGE)?.bindStorageName()
		settings.subscribe(this)
	}

	override fun onResume() {
		super.onResume()
		bindSyncSummary()
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				findPreference<Preference>(key)?.bindStorageName()
			}
			AppSettings.KEY_SUGGESTIONS -> {
				findPreference<Preference>(AppSettings.KEY_SUGGESTIONS)?.setSummary(
					if (settings.isSuggestionsEnabled) R.string.enabled else R.string.disabled
				)
			}
			AppSettings.KEY_SOURCES_HIDDEN -> {
				bindRemoteSourcesSummary()
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				val ctx = context ?: return false
				StorageSelectDialog.Builder(ctx, storageManager, this)
					.setTitle(preference.title ?: "")
					.setNegativeButton(android.R.string.cancel)
					.create()
					.show()
				true
			}
			AppSettings.KEY_SYNC -> {
				val am = AccountManager.get(requireContext())
				val accountType = getString(R.string.account_type_sync)
				val account = am.getAccountsByType(accountType).firstOrNull()
				if (account == null) {
					am.addAccount(accountType, accountType, null, null, requireActivity(), null, null)
				} else {
					try {
						startActivity(getSyncSettingsIntent(account))
					} catch (_: ActivityNotFoundException) {
						Snackbar.make(listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
					}
				}
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onStorageSelected(file: File) {
		settings.mangaStorageDir = file
	}

	private fun Preference.bindStorageName() {
		viewLifecycleScope.launch {
			val storage = storageManager.getDefaultWriteableDir()
			summary = storage?.getStorageName(context) ?: getString(R.string.not_available)
		}
	}

	private fun bindRemoteSourcesSummary() {
		findPreference<Preference>(AppSettings.KEY_REMOTE_SOURCES)?.run {
			val total = settings.remoteMangaSources.size
			summary = getString(
				R.string.enabled_d_of_d, total - settings.hiddenSources.size, total
			)
		}
	}

	private fun bindSyncSummary() {
		viewLifecycleScope.launch {
			val account = withContext(Dispatchers.Default) {
				val type = getString(R.string.account_type_sync)
				AccountManager.get(requireContext()).getAccountsByType(type).firstOrNull()
			}
			findPreference<Preference>(AppSettings.KEY_SYNC)?.run {
				summary = account?.name ?: getString(R.string.sync_title)
			}
		}
	}

	/**
	 * Some magic
	 */
	private fun getSyncSettingsIntent(account: Account): Intent {
		val args = Bundle(1)
		args.putParcelable("account", account)
		val intent = Intent("android.settings.ACCOUNT_SYNC_SETTINGS")
		@Suppress("DEPRECATION")
		intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
		return intent
	}
}