package org.koitharu.kotatsu.ui.settings.sources

import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseFragment

class SourcesSettingsFragment : BaseFragment(R.layout.fragment_settings_sources) {

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.remote_sources)
	}
}