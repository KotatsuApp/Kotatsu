package org.koitharu.kotatsu.main.ui

import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DeprecatedSinceApi
import com.google.android.material.search.SearchView

@DeprecatedSinceApi(Build.VERSION_CODES.TIRAMISU)
class SearchViewLegacyBackCallback(
	private val searchView: SearchView
) : OnBackPressedCallback(searchView.isShowing), SearchView.TransitionListener {

	override fun handleOnBackPressed() {
		searchView.hide()
	}

	override fun onStateChanged(
		searchView: SearchView,
		previousState: SearchView.TransitionState,
		newState: SearchView.TransitionState
	) {
		isEnabled = newState >= SearchView.TransitionState.SHOWING
	}
}
