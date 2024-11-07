package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.parser.EmptyMangaRepository
import org.koitharu.kotatsu.core.parser.ParserMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import java.io.File

@AndroidEntryPoint
class SourceSettingsFragment : BasePreferenceFragment(0), Preference.OnPreferenceChangeListener {

	private val viewModel: SourceSettingsViewModel by viewModels()

	override fun onResume() {
		super.onResume()
		context?.let { ctx ->
			setTitle(viewModel.source.getTitle(ctx))
		}
		viewModel.onResume()
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceManager.sharedPreferencesName = viewModel.source.name.replace(File.separatorChar, '$')
		addPreferencesFromResource(R.xml.pref_source)
		addPreferencesFromRepository(viewModel.repository)
		val isValidSource = viewModel.repository !is EmptyMangaRepository

		findPreference<SwitchPreferenceCompat>(KEY_ENABLE)?.run {
			isVisible = isValidSource
			onPreferenceChangeListener = this@SourceSettingsFragment
		}
		findPreference<Preference>(KEY_AUTH)?.run {
			val authProvider = (viewModel.repository as? ParserMangaRepository)?.getAuthProvider()
			isVisible = authProvider != null
			isEnabled = authProvider?.isAuthorized == false
		}
		findPreference<Preference>(SourceSettings.KEY_SLOWDOWN)?.isVisible = isValidSource
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
		viewModel.browserUrl.observe(viewLifecycleOwner) {
			findPreference<Preference>(AppSettings.KEY_OPEN_BROWSER)?.run {
				isVisible = it != null
				summary = it
			}
		}
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(listView))
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			KEY_AUTH -> {
				startActivity(SourceAuthActivity.newIntent(preference.context, viewModel.source))
				true
			}

			AppSettings.KEY_OPEN_BROWSER -> {
				startActivity(
					BrowserActivity.newIntent(
						context = preference.context,
						url = viewModel.browserUrl.value ?: return false,
						source = viewModel.source,
						title = viewModel.source.getTitle(preference.context),
					),
				)
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
			putString(EXTRA_SOURCE, source.name)
		}
	}
}
