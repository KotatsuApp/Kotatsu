package org.koitharu.kotatsu.base.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.base.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.prefs.AppSettings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) : PreferenceFragmentCompat(),
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
			activity?.setTitle(titleId)
		}
	}

	@CallSuper
	override fun onWindowInsetsChanged(insets: Insets) {
		listView.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom
		)
	}
}
