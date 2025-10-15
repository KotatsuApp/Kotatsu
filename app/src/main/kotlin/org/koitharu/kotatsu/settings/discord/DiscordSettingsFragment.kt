package org.koitharu.kotatsu.settings.discord

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.scrobbling.discord.ui.DiscordAuthActivity

@AndroidEntryPoint
class DiscordSettingsFragment : BasePreferenceFragment(R.string.discord) {

	private val viewModel by viewModels<DiscordSettingsViewModel>()

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_discord)
		findPreference<EditTextPreference>(AppSettings.KEY_DISCORD_TOKEN)?.let { pref ->
			pref.dialogMessage = pref.context.getString(
				R.string.discord_token_description,
				pref.context.getString(R.string.sign_in),
			)
			pref.setOnBindEditTextListener {
				it.setHint(R.string.discord_token_hint)
				it.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.tokenState.observe(viewLifecycleOwner) { (state, token) ->
			bindTokenPreference(state, token)
		}
	}

	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference is EditTextPreference && preference.key == AppSettings.KEY_DISCORD_TOKEN) {
			if (parentFragmentManager.findFragmentByTag(TokenDialogFragment.DIALOG_FRAGMENT_TAG) != null) {
				return
			}
			val f = TokenDialogFragment.newInstance(preference.key)
			@Suppress("DEPRECATION")
			f.setTargetFragment(this, 0)
			f.show(parentFragmentManager, TokenDialogFragment.DIALOG_FRAGMENT_TAG)
			return
		}
		super.onDisplayPreferenceDialog(preference)
	}

	private fun bindTokenPreference(state: TokenState, token: String?) {
		val pref = findPreference<EditTextPreference>(AppSettings.KEY_DISCORD_TOKEN) ?: return
		when (state) {
			TokenState.EMPTY -> {
				pref.icon = null
				pref.setSummary(R.string.discord_token_summary)
			}

			TokenState.REQUIRED -> {
				pref.icon = getWarningIcon()
				pref.setSummary(R.string.discord_token_summary)
			}

			TokenState.INVALID -> {
				pref.icon = getWarningIcon()
				pref.summary = getString(R.string.invalid_token, token)
			}

			TokenState.VALID -> {
				pref.icon = null
				pref.summary = token
			}

			TokenState.CHECKING -> {
				pref.icon = null
				pref.setSummary(R.string.loading_)
			}
		}
	}

	class TokenDialogFragment : EditTextPreferenceDialogFragmentCompat() {

		override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
			super.onPrepareDialogBuilder(builder)
			builder.setNeutralButton(R.string.sign_in) { _, _ ->
				openSignIn()
			}
		}

		private fun openSignIn() {
			activity?.run {
				startActivity(Intent(this, DiscordAuthActivity::class.java))
			}
		}

		companion object {

			const val DIALOG_FRAGMENT_TAG: String = "androidx.preference.PreferenceFragment.DIALOG"

			fun newInstance(key: String) = TokenDialogFragment().withArgs(1) {
				putString(ARG_KEY, key)
			}
		}
	}
}
