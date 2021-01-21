package org.koitharu.kotatsu.base.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.core.prefs.AppSettings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(), OnApplyWindowInsetsListener {

	protected val settings by inject<AppSettings>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		listView.clipToPadding = false
		ViewCompat.setOnApplyWindowInsetsListener(view, this)
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(titleId)
	}

	override fun onApplyWindowInsets(v: View?, insets: WindowInsetsCompat): WindowInsetsCompat {
		val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		listView.updatePadding(
			left = systemBars.left,
			right = systemBars.right,
			bottom = systemBars.bottom
		)
		return insets
	}
}