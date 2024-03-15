package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity

@AndroidEntryPoint
class SourceSettingsFragment : BasePreferenceFragment(0), Preference.OnPreferenceChangeListener {

	private val viewModel: SourceSettingsViewModel by viewModels()
	private val exceptionResolver = ExceptionResolver(this)

	override fun onResume() {
		super.onResume()
		setTitle(viewModel.source.title)
		viewModel.onResume()
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceManager.sharedPreferencesName = viewModel.source.name
		addPreferencesFromResource(R.xml.pref_source)
		addPreferencesFromRepository(viewModel.repository)

		findPreference<SwitchPreferenceCompat>(KEY_ENABLE)?.run {
			setOnPreferenceChangeListener(this@SourceSettingsFragment)
		}
		findPreference<Preference>(KEY_AUTH)?.run {
			val authProvider = viewModel.repository.getAuthProvider()
			isVisible = authProvider != null
			isEnabled = authProvider?.isAuthorized == false
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.username.observe(viewLifecycleOwner) { username ->
			findPreference<Preference>(KEY_AUTH)?.summary = username?.let {
				getString(R.string.logged_in_as, it)
			}
		}
		viewModel.onError.observeEvent(
			viewLifecycleOwner,
			SnackbarErrorObserver(
				listView,
				this,
				exceptionResolver,
			) { viewModel.onResume() },
		)
		viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
			findPreference<Preference>(KEY_AUTH)?.isEnabled = !isLoading
		}
		viewModel.isEnabled.observe(viewLifecycleOwner) { enabled ->
			findPreference<SwitchPreferenceCompat>(KEY_ENABLE)?.isChecked = enabled
		}
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(listView))
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			KEY_AUTH -> {
				startActivity(SourceAuthActivity.newIntent(preference.context, viewModel.source))
				true
			}

			AppSettings.KEY_COOKIES_CLEAR -> {
				viewModel.clearCookies()
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
		when (preference.key) {
			KEY_ENABLE -> viewModel.setEnabled(newValue == true)
			else -> return false
		}
		return true
	}

	companion object {

		private const val KEY_AUTH = "auth"
		private const val KEY_ENABLE = "enable"

		const val EXTRA_SOURCE = "source"

		fun newInstance(source: MangaSource) = SourceSettingsFragment().withArgs(1) {
			putSerializable(EXTRA_SOURCE, source)
		}
	}
}
