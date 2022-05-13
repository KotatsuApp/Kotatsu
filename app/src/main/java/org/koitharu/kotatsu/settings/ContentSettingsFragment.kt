package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import kotlinx.coroutines.launch
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
}