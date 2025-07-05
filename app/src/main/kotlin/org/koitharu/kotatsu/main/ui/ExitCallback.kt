package org.koitharu.kotatsu.main.ui

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner

class ExitCallback(
	private val activity: MainActivity,
	private val snackbarHost: View,
) : OnBackPressedCallback(false), SearchView.TransitionListener {

	private var job: Job? = null
	private val isSearchOpen = MutableStateFlow(activity.viewBinding.searchView.isShowing)
	private val isDisabledByTimeout = MutableStateFlow(false)

	init {
		activity.lifecycleScope.launch {
			combine(
				observeSettings(),
				isSearchOpen,
				isDisabledByTimeout,
			) { enabledInSettings, searchOpen, disabledTemporary ->
				enabledInSettings && !searchOpen && !disabledTemporary
			}.collect {
				isEnabled = it
			}
		}
	}

	override fun handleOnBackPressed() {
		job?.cancel()
		job = activity.lifecycleScope.launch {
			resetExitConfirmation()
		}
	}

	override fun onStateChanged(
		searchView: SearchView,
		previousState: SearchView.TransitionState,
		newState: SearchView.TransitionState
	) {
		isSearchOpen.value = newState >= SearchView.TransitionState.SHOWING
	}

	private suspend fun resetExitConfirmation() {
		isDisabledByTimeout.value = true
		val snackbar = Snackbar.make(snackbarHost, R.string.confirm_exit, Snackbar.LENGTH_INDEFINITE)
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		snackbar.show()
		delay(2000)
		snackbar.dismiss()
		isDisabledByTimeout.value = false
	}

	private fun observeSettings(): Flow<Boolean> = activity.settings
		.observeAsFlow(AppSettings.KEY_EXIT_CONFIRM) { isExitConfirmationEnabled }
		.flowOn(Dispatchers.Default)
}
