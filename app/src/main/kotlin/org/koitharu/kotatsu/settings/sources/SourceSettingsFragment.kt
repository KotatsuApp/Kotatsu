package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity

@AndroidEntryPoint
class SourceSettingsFragment : BasePreferenceFragment(0) {

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
		viewModel.onError.observeEvent(viewLifecycleOwner, ::onError)
		viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
			findPreference<Preference>(KEY_AUTH)?.isEnabled = !isLoading
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			KEY_AUTH -> {
				startActivity(SourceAuthActivity.newIntent(preference.context, viewModel.source))
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun onError(error: Throwable) {
		val snackbar = Snackbar.make(
			listView ?: return,
			error.getDisplayMessage(resources),
			Snackbar.LENGTH_INDEFINITE,
		)
		if (ExceptionResolver.canResolve(error)) {
			snackbar.setAction(ExceptionResolver.getResolveStringId(error)) { resolveError(error) }
		}
		snackbar.show()
	}

	private fun resolveError(error: Throwable) {
		view ?: return
		viewLifecycleScope.launch {
			if (exceptionResolver.resolve(error)) {
				viewModel.onResume()
			}
		}
	}

	companion object {

		private const val KEY_AUTH = "auth"

		const val EXTRA_SOURCE = "source"

		fun newInstance(source: MangaSource) = SourceSettingsFragment().withArgs(1) {
			putSerializable(EXTRA_SOURCE, source)
		}
	}
}
