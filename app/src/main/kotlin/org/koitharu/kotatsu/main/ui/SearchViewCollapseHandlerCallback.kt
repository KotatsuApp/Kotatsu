package org.koitharu.kotatsu.main.ui

import androidx.activity.OnBackPressedCallback
import com.google.android.material.search.SearchView

class SearchViewCollapseHandlerCallback(
	private val searchView: SearchView
) : OnBackPressedCallback(false), SearchView.TransitionListener {

	init {
		searchView.addTransitionListener(this)
		isEnabled = searchView.isShowing
	}

	override fun handleOnBackPressed() {
		if (searchView.isShowing) {
			searchView.hide()
		}
	}

	override fun onStateChanged(
		searchView: SearchView,
		previousState: SearchView.TransitionState,
		newState: SearchView.TransitionState
	) {
		isEnabled = (newState == SearchView.TransitionState.SHOWING || newState == SearchView.TransitionState.SHOWN)
	}
}
