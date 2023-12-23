package org.koitharu.kotatsu.core.ui.util

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.LinkedList

class WindowInsetsDelegate : OnApplyWindowInsetsListener, View.OnLayoutChangeListener {

	@JvmField
	var handleImeInsets: Boolean = false

	@JvmField
	var interceptingWindowInsetsListener: OnApplyWindowInsetsListener? = null

	private val listeners = LinkedList<WindowInsetsListener>()
	private var lastInsets: Insets? = null

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val handledInsets = interceptingWindowInsetsListener?.onApplyWindowInsets(v, insets) ?: insets
		val newInsets = if (handleImeInsets) {
			Insets.max(
				handledInsets.getInsets(WindowInsetsCompat.Type.systemBars()),
				handledInsets.getInsets(WindowInsetsCompat.Type.ime()),
			)
		} else {
			handledInsets.getInsets(WindowInsetsCompat.Type.systemBars())
		}
		if (newInsets != lastInsets) {
			listeners.forEach { it.onWindowInsetsChanged(newInsets) }
			lastInsets = newInsets
		}
		return handledInsets
	}

	override fun onLayoutChange(
		view: View,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int,
		oldLeft: Int,
		oldTop: Int,
		oldRight: Int,
		oldBottom: Int,
	) {
		view.removeOnLayoutChangeListener(this)
		if (lastInsets == null) { // Listener may not be called
			onApplyWindowInsets(view, ViewCompat.getRootWindowInsets(view) ?: return)
		}
	}

	fun addInsetsListener(listener: WindowInsetsListener) {
		listeners.add(listener)
		lastInsets?.let { listener.onWindowInsetsChanged(it) }
	}

	fun removeInsetsListener(listener: WindowInsetsListener) {
		listeners.remove(listener)
	}

	fun onViewCreated(view: View) {
		ViewCompat.setOnApplyWindowInsetsListener(view, this)
		view.addOnLayoutChangeListener(this)
	}

	fun onDestroyView() {
		lastInsets = null
	}

	fun interface WindowInsetsListener {

		fun onWindowInsetsChanged(insets: Insets)
	}
}
