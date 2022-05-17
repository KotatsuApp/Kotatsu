package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceHeaderFragmentCompat
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import org.koitharu.kotatsu.R

class SettingsHeadersFragment : PreferenceHeaderFragmentCompat(), SlidingPaneLayout.PanelSlideListener {

	private var currentTitle: CharSequence? = null

	override fun onCreatePreferenceHeader(): PreferenceFragmentCompat = RootSettingsFragment()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		slidingPaneLayout.addPanelSlideListener(this)
	}

	override fun onPanelSlide(panel: View, slideOffset: Float) = Unit

	override fun onPanelOpened(panel: View) {
		activity?.title = currentTitle ?: getString(R.string.settings)
	}

	override fun onPanelClosed(panel: View) {
		activity?.setTitle(R.string.settings)
	}

	fun setTitle(title: CharSequence?) {
		currentTitle = title
		if (slidingPaneLayout.isOpen) {
			activity?.title = title
		}
	}

	fun openFragment(fragment: Fragment) {
		childFragmentManager.commit {
			setReorderingAllowed(true)
			replace(androidx.preference.R.id.preferences_detail, fragment)
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			addToBackStack(null)
		}
	}
}