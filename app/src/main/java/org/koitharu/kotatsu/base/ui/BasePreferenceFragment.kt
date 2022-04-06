package org.koitharu.kotatsu.base.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.base.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.SettingsHeadersFragment

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(),
	WindowInsetsDelegate.WindowInsetsListener,
	RecyclerViewOwner {

	protected val settings by inject<AppSettings>(mode = LazyThreadSafetyMode.NONE)

	@Suppress("LeakingThis")
	protected val insetsDelegate = WindowInsetsDelegate(this)

	override val recyclerView: RecyclerView
		get() = listView

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		listView.clipToPadding = false
		insetsDelegate.onViewCreated(view)
	}

	override fun onDestroyView() {
		insetsDelegate.onDestroyView()
		super.onDestroyView()
	}

	override fun onResume() {
		super.onResume()
		if (titleId != 0) {
			setTitle(getString(titleId))
		}
	}

	@CallSuper
	override fun onWindowInsetsChanged(insets: Insets) {
		listView.updatePadding(
			bottom = insets.bottom
		)
	}

	@Suppress("UsePropertyAccessSyntax")
	protected fun setTitle(title: CharSequence) {
		(parentFragment as? SettingsHeadersFragment)?.setTitle(title)
			?: activity?.setTitle(title)
	}
}