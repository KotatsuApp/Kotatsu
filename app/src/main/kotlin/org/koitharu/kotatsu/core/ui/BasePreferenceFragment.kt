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
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.ui.util.WindowInsetsDelegate
import javax.inject.Inject

@Suppress("LeakingThis")
@AndroidEntryPoint
abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(),
	WindowInsetsDelegate.WindowInsetsListener,
	RecyclerViewOwner {

	@Inject
	lateinit var settings: AppSettings

	@JvmField
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
			bottom = insets.bottom,
		)
	}

	protected fun setTitle(title: CharSequence) {
		activity?.let {
			if (!it.resources.getBoolean(R.bool.is_tablet)) {
				it.title = title
			}
		}
	}
}
