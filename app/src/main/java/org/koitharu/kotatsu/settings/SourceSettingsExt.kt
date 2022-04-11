package org.koitharu.kotatsu.settings

import android.view.inputmethod.EditorInfo
import androidx.preference.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.settings.utils.EditTextBindListener
import org.koitharu.kotatsu.settings.utils.EditTextDefaultSummaryProvider
import org.koitharu.kotatsu.utils.ext.setDefaultValueCompat

private const val KEY_DOMAIN = "domain"

fun PreferenceFragmentCompat.addPreferencesFromRepository(repository: RemoteMangaRepository) {
	val configKeys = repository.getConfigKeys()
	val screen = preferenceScreen
	for (key in configKeys) {
		val preference: Preference = when (key) {
			is ConfigKey.Domain -> {
				val presetValues = key.presetValues
				if (presetValues.isNullOrEmpty()) {
					EditTextPreference(requireContext()).apply {
						summaryProvider = EditTextDefaultSummaryProvider(key.defaultValue)
						setOnBindEditTextListener(
							EditTextBindListener(
								inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI,
								hint = key.defaultValue,
							)
						)
					}
				} else {
					DropDownPreference(requireContext()).apply {
						entries = presetValues
						entryValues = entries
						summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
						setDefaultValueCompat(key.defaultValue)
					}
				}.apply {
					setTitle(R.string.domain)
					setDialogTitle(R.string.domain)
				}
			}
		}
		preference.isIconSpaceReserved = false
		preference.key = key.key
		screen.addPreference(preference)
	}
}