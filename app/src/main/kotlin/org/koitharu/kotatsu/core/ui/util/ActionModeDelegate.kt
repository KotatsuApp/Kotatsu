package org.koitharu.kotatsu.core.ui.util

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ActionModeDelegate : OnBackPressedCallback(false) {

	private var activeActionMode: ActionMode? = null
	private var listeners: MutableList<ActionModeListener>? = null

	val isActionModeStarted: Boolean
		get() = activeActionMode != null

	override fun handleOnBackPressed() {
		finishActionMode()
	}

	fun onSupportActionModeStarted(mode: ActionMode) {
		activeActionMode = mode
		isEnabled = true
		listeners?.forEach { it.onActionModeStarted(mode) }
	}

	fun onSupportActionModeFinished(mode: ActionMode) {
		activeActionMode = null
		isEnabled = false
		listeners?.forEach { it.onActionModeFinished(mode) }
	}

	fun addListener(listener: ActionModeListener) {
		if (listeners == null) {
			listeners = ArrayList()
		}
		checkNotNull(listeners).add(listener)
	}

	fun removeListener(listener: ActionModeListener) {
		listeners?.remove(listener)
	}

	fun addListener(listener: ActionModeListener, owner: LifecycleOwner) {
		addListener(listener)
		owner.lifecycle.addObserver(ListenerLifecycleObserver(listener))
	}

	fun finishActionMode() {
		activeActionMode?.finish()
	}

	private inner class ListenerLifecycleObserver(
		private val listener: ActionModeListener,
	) : DefaultLifecycleObserver {

		override fun onDestroy(owner: LifecycleOwner) {
			super.onDestroy(owner)
			removeListener(listener)
		}
	}
}
