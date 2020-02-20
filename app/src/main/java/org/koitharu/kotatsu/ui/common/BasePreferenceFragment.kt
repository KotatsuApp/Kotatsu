package org.koitharu.kotatsu.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.prefs.AppSettings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(), KoinComponent {

	protected val settings by inject<AppSettings>()

	override fun onAttach(context: Context) {
		super.onAttach(context)
		activity?.setTitle(titleId)
	}

	fun <T : Preference> findPreference(@StringRes keyId: Int): T? =
		findPreference(getString(keyId))

}