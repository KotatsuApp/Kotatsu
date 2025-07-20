package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.settings.utils.EditTextFallbackSummaryProvider

class DiscordSettingsFragment : BasePreferenceFragment(R.string.discord) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_discord)
		findPreference<EditTextPreference>(AppSettings.KEY_DISCORD_TOKEN)?.let { pref ->
			pref.summaryProvider = EditTextFallbackSummaryProvider(R.string.discord_token_summary)
			pref.setDialogMessage(R.string.discord_token_summary)
			pref.setOnBindEditTextListener {
				it.setHint(R.string.discord_token_hint)
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.observe(
			AppSettings.KEY_DISCORD_RPC,
			AppSettings.KEY_DISCORD_TOKEN,
		).observe(viewLifecycleOwner) {
			bindTokenWarning()
		}
	}

	private fun bindTokenWarning() {
		val pref = findPreference<EditTextPreference>(AppSettings.KEY_DISCORD_TOKEN) ?: return
		val shouldShowError = settings.isDiscordRpcEnabled && settings.discordToken == null
		pref.icon = if (shouldShowError) {
			getWarningIcon()
		} else {
			null
		}
	}
}
