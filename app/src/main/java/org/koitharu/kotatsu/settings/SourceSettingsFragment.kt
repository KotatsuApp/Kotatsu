package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import org.koitharu.kotatsu.utils.ext.serializableArgument
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import org.koitharu.kotatsu.utils.ext.withArgs

class SourceSettingsFragment : BasePreferenceFragment(0) {

	private val source by serializableArgument<MangaSource>(EXTRA_SOURCE)
	private var repository: RemoteMangaRepository? = null

	override fun onResume() {
		super.onResume()
		activity?.title = source.title
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceManager.sharedPreferencesName = source.name
		val repo = MangaRepository(source) as? RemoteMangaRepository ?: return
		repository = repo
		addPreferencesFromResource(R.xml.pref_source)
		addPreferencesFromRepository(repo)

		findPreference<Preference>(KEY_AUTH)?.run {
			val authProvider = repo.getAuthProvider()
			isVisible = authProvider != null
			isEnabled = authProvider?.isAuthorized == false
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(KEY_AUTH)?.run {
			if (isVisible) {
				loadUsername(this)
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			KEY_AUTH -> {
				startActivity(SourceAuthActivity.newIntent(preference.context, source))
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun loadUsername(preference: Preference) = viewLifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.Default) {
				requireNotNull(repository?.getAuthProvider()?.getUsername())
			}
		}.onSuccess { username ->
			preference.title = getString(R.string.logged_in_as, username)
		}.onFailure { error ->
			preference.isEnabled = error is AuthRequiredException
			if (BuildConfig.DEBUG) {
				error.printStackTrace()
			}
		}
	}

	companion object {

		private const val KEY_AUTH = "auth"

		private const val EXTRA_SOURCE = "source"

		fun newInstance(source: MangaSource) = SourceSettingsFragment().withArgs(1) {
			putSerializable(EXTRA_SOURCE, source)
		}
	}
}