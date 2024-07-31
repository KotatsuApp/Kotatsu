package org.koitharu.kotatsu.settings.sources

import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.EmptyMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.ParserMangaRepository
import org.koitharu.kotatsu.core.util.ext.mapToArray
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.settings.utils.AutoCompleteTextViewPreference
import org.koitharu.kotatsu.settings.utils.EditTextBindListener
import org.koitharu.kotatsu.settings.utils.EditTextDefaultSummaryProvider
import org.koitharu.kotatsu.settings.utils.validation.DomainValidator
import org.koitharu.kotatsu.settings.utils.validation.HeaderValidator

fun PreferenceFragmentCompat.addPreferencesFromRepository(repository: MangaRepository) = when (repository) {
	is ParserMangaRepository -> addPreferencesFromParserRepository(repository)
	is EmptyMangaRepository -> addPreferencesFromEmptyRepository()
	else -> Unit
}

private fun PreferenceFragmentCompat.addPreferencesFromParserRepository(repository: ParserMangaRepository) {
	addPreferencesFromResource(R.xml.pref_source_parser)
	val configKeys = repository.getConfigKeys()
	val screen = preferenceScreen
	for (key in configKeys) {
		val preference: Preference = when (key) {
			is ConfigKey.Domain -> {
				val presetValues = key.presetValues
				if (presetValues.size <= 1) {
					EditTextPreference(screen.context)
				} else {
					AutoCompleteTextViewPreference(screen.context).apply {
						entries = presetValues.toStringArray()
					}
				}.apply {
					summaryProvider = EditTextDefaultSummaryProvider(key.defaultValue)
					setOnBindEditTextListener(
						EditTextBindListener(
							inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI,
							hint = key.defaultValue,
							validator = DomainValidator(),
						),
					)
					setTitle(R.string.domain)
					setDialogTitle(R.string.domain)
				}
			}

			is ConfigKey.UserAgent -> {
				AutoCompleteTextViewPreference(screen.context).apply {
					entries = arrayOf(
						UserAgents.FIREFOX_MOBILE,
						UserAgents.CHROME_MOBILE,
						UserAgents.FIREFOX_DESKTOP,
						UserAgents.CHROME_DESKTOP,
					)
					summaryProvider = EditTextDefaultSummaryProvider(key.defaultValue)
					setOnBindEditTextListener(
						EditTextBindListener(
							inputType = EditorInfo.TYPE_CLASS_TEXT,
							hint = key.defaultValue,
							validator = HeaderValidator(),
						),
					)
					setTitle(R.string.user_agent)
					setDialogTitle(R.string.user_agent)
				}
			}

			is ConfigKey.ShowSuspiciousContent -> {
				SwitchPreferenceCompat(screen.context).apply {
					setDefaultValue(key.defaultValue)
					setTitle(R.string.show_suspicious_content)
				}
			}

			is ConfigKey.SplitByTranslations -> {
				SwitchPreferenceCompat(screen.context).apply {
					setDefaultValue(key.defaultValue)
					setTitle(R.string.split_by_translations)
					setSummary(R.string.split_by_translations_summary)
				}
			}

			is ConfigKey.PreferredImageServer -> {
				ListPreference(screen.context).apply {
					entries = key.presetValues.values.mapToArray {
						it ?: context.getString(R.string.automatic)
					}
					entryValues = key.presetValues.keys.mapToArray { it.orEmpty() }
					setDefaultValue(key.defaultValue.orEmpty())
					setTitle(R.string.image_server)
					setDialogTitle(R.string.image_server)
					summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
				}
			}
		}
		preference.isIconSpaceReserved = false
		preference.key = key.key
		preference.order = 10
		screen.addPreference(preference)
	}
}

private fun PreferenceFragmentCompat.addPreferencesFromEmptyRepository() {
	val preference = Preference(requireContext())
	preference.setIcon(R.drawable.ic_alert_outline)
	preference.isPersistent = false
	preference.isSelectable = false
	preference.order = 200
	preference.setSummary(R.string.unsupported_source)
	preferenceScreen.addPreference(preference)
}

private fun Array<out String>.toStringArray(): Array<String> {
	return Array(size) { i -> this[i] as? String ?: "" }
}
