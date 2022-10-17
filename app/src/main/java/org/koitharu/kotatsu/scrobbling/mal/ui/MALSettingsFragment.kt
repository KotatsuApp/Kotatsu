package org.koitharu.kotatsu.scrobbling.mal.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.scrobbling.mal.data.model.MALUser
import org.koitharu.kotatsu.utils.ext.assistedViewModels
import org.koitharu.kotatsu.utils.ext.withArgs
import javax.inject.Inject

@AndroidEntryPoint
class MALSettingsFragment : BasePreferenceFragment(R.string.mal) {

	@Inject
	lateinit var viewModelFactory: MALSettingsViewModel.Factory

	private val viewModel by assistedViewModels {
		viewModelFactory.create(arguments?.getString(ARG_AUTH_CODE))
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_mal)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.user.observe(viewLifecycleOwner, this::onUserChanged)
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			KEY_USER -> openAuthorization()
			KEY_LOGOUT -> {
				viewModel.logout()
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun onUserChanged(user: MALUser?) {
		val pref = findPreference<Preference>(KEY_USER) ?: return
		pref.isSelectable = user == null
		pref.title = user?.nickname ?: getString(R.string.sign_in)
		findPreference<Preference>(KEY_LOGOUT)?.isVisible = user != null
	}

	private fun openAuthorization(): Boolean {
		return runCatching {
			val intent = Intent(Intent.ACTION_VIEW)
			intent.data = Uri.parse(viewModel.authorizationUrl)
			startActivity(intent)
		}.isSuccess
	}

	companion object {

		private const val KEY_USER = "mal_user"
		private const val KEY_LOGOUT = "mal_logout"

		private const val ARG_AUTH_CODE = "auth_code"

		fun newInstance(authCode: String?) = MALSettingsFragment().withArgs(1) {
			putString(ARG_AUTH_CODE, authCode)
		}
	}
}

