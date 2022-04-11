package org.koitharu.kotatsu.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.base.ui.dialog.StorageSelectDialog
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.shikimori.data.ShikimoriRepository
import org.koitharu.kotatsu.utils.ext.getStorageName
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

class ContentSettingsFragment :
	BasePreferenceFragment(R.string.content),
	SharedPreferences.OnSharedPreferenceChangeListener,
	StorageSelectDialog.OnStorageSelectListener {

	private val storageManager by inject<LocalStorageManager>()
	private val shikimoriRepository by inject<ShikimoriRepository>(mode = LazyThreadSafetyMode.NONE)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_content)

		findPreference<Preference>(AppSettings.KEY_SUGGESTIONS)?.setSummary(
			if (settings.isSuggestionsEnabled) R.string.enabled else R.string.disabled
		)
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
			AppSettings.KEY_SHIKIMORI -> {
				if (!shikimoriRepository.isAuthorized) {
					showShikimoriDialog()
					true
				} else {
					super.onPreferenceTreeClick(preference)
				}
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
			val total = MangaSource.values().size - 1
			summary = getString(R.string.enabled_d_of_d, total - settings.hiddenSources.size, total)
		}
	}

	private fun showShikimoriDialog() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.shikimori)
			.setMessage(R.string.shikimori_info)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.sign_in) { _, _ ->
				runCatching {
					val intent = Intent(Intent.ACTION_VIEW)
					intent.data = Uri.parse(shikimoriRepository.oauthUrl)
					startActivity(intent)
				}.onFailure {
					Snackbar.make(listView, R.string.operation_not_supported, Snackbar.LENGTH_LONG).show()
				}
			}.show()
	}
}