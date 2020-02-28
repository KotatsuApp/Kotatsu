package org.koitharu.kotatsu.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.utils.ext.withArgs

class SourceSettingsFragment : PreferenceFragmentCompat(), KoinComponent {

	private lateinit var source: MangaSource

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		source = requireArguments().getParcelable(EXTRA_SOURCE)!!
	}

	override fun onResume() {
		super.onResume()
		activity?.title = source.title
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

	}

	companion object {

		private const val EXTRA_SOURCE = "source"

		fun newInstance(source: MangaSource) = SourceSettingsFragment().withArgs(1) {
			putParcelable(EXTRA_SOURCE, source)
		}
	}
}