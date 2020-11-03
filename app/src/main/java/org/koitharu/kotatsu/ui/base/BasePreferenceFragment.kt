package org.koitharu.kotatsu.ui.base

import androidx.annotation.StringRes
import androidx.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.core.prefs.AppSettings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat() {

	protected val settings by inject<AppSettings>()

	override fun onResume() {
		super.onResume()
		activity?.setTitle(titleId)
	}
}