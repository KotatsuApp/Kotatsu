package org.koitharu.kotatsu.ui.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.ui.settings.utils.EditTextSummaryProvider
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
		val repo = source.repository as? RemoteMangaRepository ?: return
		val keys = repo.onCreatePreferences().map(::getString)
		addPreferencesFromResource(R.xml.pref_source)
		for (i in 0 until preferenceScreen.preferenceCount) {
			val pref = preferenceScreen.getPreference(i)
			pref.isVisible = pref.key in keys
		}
		findPreference<EditTextPreference>(getString(R.string.key_parser_domain))?.summaryProvider =
			EditTextSummaryProvider(R.string._default)
	}

	companion object {

		private const val EXTRA_SOURCE = "source"

		fun newInstance(source: MangaSource) = SourceSettingsFragment().withArgs(1) {
			putParcelable(EXTRA_SOURCE, source)
		}
	}
}