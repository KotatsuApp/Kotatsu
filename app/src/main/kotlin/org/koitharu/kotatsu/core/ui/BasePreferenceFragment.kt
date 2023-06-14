package org.koitharu.kotatsu.core.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.settings.SettingsActivity
import javax.inject.Inject

@AndroidEntryPoint
abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(),
	WindowInsetsDelegate.WindowInsetsListener,
	RecyclerViewOwner {

	@Inject
	lateinit var settings: AppSettings

	@JvmField
	protected val insetsDelegate = WindowInsetsDelegate()

	override val recyclerView: RecyclerView
		get() = listView

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.setBackgroundColor(view.context.getThemeColor(android.R.attr.colorBackground))
		listView.clipToPadding = false
		insetsDelegate.onViewCreated(view)
		insetsDelegate.addInsetsListener(this)
	}

	override fun onDestroyView() {
		insetsDelegate.removeInsetsListener(this)
		insetsDelegate.onDestroyView()
		super.onDestroyView()
	}

	override fun onResume() {
		super.onResume()
		setTitle(if (titleId != 0) getString(titleId) else null)
	}

	@CallSuper
	override fun onWindowInsetsChanged(insets: Insets) {
		listView.updatePadding(
			bottom = insets.bottom,
		)
	}

	protected fun setTitle(title: CharSequence?) {
		(activity as? SettingsActivity)?.setSectionTitle(title)
	}
}
