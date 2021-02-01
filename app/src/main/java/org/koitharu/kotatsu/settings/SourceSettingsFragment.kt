package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.util.ArrayMap
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.settings.utils.EditTextBindListener
import org.koitharu.kotatsu.settings.utils.EditTextDefaultSummaryProvider
import org.koitharu.kotatsu.utils.ext.mangaRepositoryOf
import org.koitharu.kotatsu.utils.ext.parcelableArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class SourceSettingsFragment : PreferenceFragmentCompat() {

	private val source by parcelableArgument<MangaSource>(EXTRA_SOURCE)

	override fun onResume() {
		super.onResume()
		activity?.title = source.title
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceManager.sharedPreferencesName = source.name
		val repo = mangaRepositoryOf(source) as? RemoteMangaRepository ?: return
		addPreferencesFromResource(R.xml.pref_source)
		val screen = preferenceScreen
		val prefsMap = ArrayMap<String, Any>(screen.preferenceCount)
		repo.onCreatePreferences(prefsMap)
		for (i in 0 until screen.preferenceCount) {
			val pref = screen.getPreference(i)
			val defValue = prefsMap[pref.key]
			pref.isVisible = defValue != null
			if (defValue != null) {
				initPreferenceWithDefaultValue(pref, defValue)
			}
		}
	}

	private fun initPreferenceWithDefaultValue(preference: Preference, defaultValue: Any) {
		when(preference) {
			is EditTextPreference -> {
				preference.summaryProvider = EditTextDefaultSummaryProvider(defaultValue.toString())
				when(preference.key) {
					SourceSettings.KEY_DOMAIN -> preference.setOnBindEditTextListener(
						EditTextBindListener(
							EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI,
							defaultValue.toString()
						)
					)
				}
			}
			is TwoStatePreference -> {
				if (defaultValue is Boolean) {
					preference.isChecked = defaultValue
				}
			}
		}
	}

	companion object {

		private const val EXTRA_SOURCE = "source"

		fun newInstance(source: MangaSource) = SourceSettingsFragment().withArgs(1) {
			putParcelable(EXTRA_SOURCE, source)
		}
	}
}