package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.settings.utils.EditTextSummaryProvider
import org.koitharu.kotatsu.utils.ext.mangaRepositoryOf
import org.koitharu.kotatsu.utils.ext.withArgs

class SourceSettingsFragment : PreferenceFragmentCompat() {

	private val source by lazy(LazyThreadSafetyMode.NONE) {
		requireArguments().getParcelable<MangaSource>(EXTRA_SOURCE)!!
	}

	override fun onResume() {
		super.onResume()
		activity?.title = source.title
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceManager.sharedPreferencesName = source.name
		val repo = mangaRepositoryOf(source) as? RemoteMangaRepository ?: return
		val keys = repo.onCreatePreferences()
		addPreferencesFromResource(R.xml.pref_source)
		for (i in 0 until preferenceScreen.preferenceCount) {
			val pref = preferenceScreen.getPreference(i)
			pref.isVisible = pref.key in keys
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<EditTextPreference>(SourceSettings.KEY_DOMAIN)?.summaryProvider =
			EditTextSummaryProvider(R.string._default)
	}

	companion object {

		private const val EXTRA_SOURCE = "source"

		fun newInstance(source: MangaSource) = SourceSettingsFragment().withArgs(1) {
			putParcelable(EXTRA_SOURCE, source)
		}
	}
}