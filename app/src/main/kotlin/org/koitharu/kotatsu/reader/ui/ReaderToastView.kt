package org.koitharu.kotatsu.reader.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.StringRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.textview.MaterialTextView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled

class ReaderToastView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : MaterialTextView(context, attrs, defStyleAttr) {

	private var currentAnimator: ViewPropertyAnimator? = null

	private var hideRunnable = Runnable {
		hide()
	}

	fun show(message: CharSequence) {
		removeCallbacks(hideRunnable)
		text = message
		showImpl()
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
		hideImpl()
	}

	override fun onDetachedFromWindow() {
		removeCallbacks(hideRunnable)
		super.onDetachedFromWindow()
	}

	private fun showImpl() {
		currentAnimator?.cancel()
		clearAnimation()
		if (!context.isAnimationsEnabled) {
			currentAnimator = null
			isVisible = true
			return
		}
		alpha = 0f
		isVisible = true
		currentAnimator = animate()
			.alpha(1f)
			.setInterpolator(DecelerateInterpolator())
			.setDuration(context.getAnimationDuration(R.integer.config_shorterAnimTime))
			.setListener(null)
	}

	private fun hideImpl() {
		currentAnimator?.cancel()
		clearAnimation()
		if (!context.isAnimationsEnabled) {
			currentAnimator = null
			isGone = true
			return
		}
		currentAnimator = animate()
			.alpha(0f)
			.setInterpolator(AccelerateInterpolator())
			.setDuration(context.getAnimationDuration(R.integer.config_shorterAnimTime))
			.setListener(
				object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: Animator) {
						isGone = true
					}
				},
			)
	}
}
