package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.textview.MaterialTextView

class ReaderToastView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : MaterialTextView(context, attrs, defStyleAttr) {

	private var hideRunnable = Runnable {
		hide()
	}

	fun show(message: CharSequence) {
		removeCallbacks(hideRunnable)
		text = message
		setupTransition()
		isVisible = true
	}

	fun show(@StringRes messageId: Int) {
		show(context.getString(messageId))
	}

	fun showTemporary(message: CharSequence, duration: Long) {
		show(message)
		postDelayed(hideRunnable, duration)
	}

	fun hide() {
		removeCallbacks(hideRunnable)
		setupTransition()
		isVisible = false
	}

	override fun onDetachedFromWindow() {
		removeCallbacks(hideRunnable)
		super.onDetachedFromWindow()
	}

	private fun setupTransition() {
		val parentView = parent as? ViewGroup ?: return
		val transition = TransitionSet()
			.setOrdering(TransitionSet.ORDERING_TOGETHER)
			.addTarget(this)
			.addTransition(Slide(Gravity.BOTTOM))
			.addTransition(Fade())
		TransitionManager.beginDelayedTransition(parentView, transition)
	}
}