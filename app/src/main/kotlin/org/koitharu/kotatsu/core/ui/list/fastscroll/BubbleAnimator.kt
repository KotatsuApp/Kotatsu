package org.koitharu.kotatsu.core.ui.list.fastscroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import org.koitharu.kotatsu.core.util.ext.animatorDurationScale
import org.koitharu.kotatsu.core.util.ext.measureWidth
import kotlin.math.hypot

class BubbleAnimator(
	private val bubble: View,
) {

	private val animationDuration = (
		bubble.resources.getInteger(android.R.integer.config_shortAnimTime) *
			bubble.context.animatorDurationScale
		).toLong()
	private var animator: Animator? = null
	private var isHiding = false

	fun show() {
		if (bubble.isVisible && !isHiding) {
			return
		}
		isHiding = false
		animator?.cancel()
		animator = ViewAnimationUtils.createCircularReveal(
			bubble,
			bubble.measureWidth(),
			bubble.measuredHeight,
			0f,
			hypot(bubble.width.toDouble(), bubble.height.toDouble()).toFloat(),
		).apply {
			bubble.isVisible = true
			duration = animationDuration
			interpolator = DecelerateInterpolator()
			start()
		}
	}

	fun hide() {
		if (!bubble.isVisible || isHiding) {
			return
		}
		animator?.cancel()
		isHiding = true
		animator = ViewAnimationUtils.createCircularReveal(
			bubble,
			bubble.width,
			bubble.height,
			hypot(bubble.width.toDouble(), bubble.height.toDouble()).toFloat(),
			0f,
		).apply {
			duration = animationDuration
			interpolator = AccelerateInterpolator()
			addListener(HideListener())
			start()
		}
	}

	private inner class HideListener : AnimatorListenerAdapter() {

		private var isCancelled = false

		override fun onAnimationCancel(animation: Animator) {
			super.onAnimationCancel(animation)
			isCancelled = true
		}

		override fun onAnimationEnd(animation: Animator) {
			super.onAnimationEnd(animation)
			if (!isCancelled && animation === this@BubbleAnimator.animator) {
				bubble.isInvisible = true
				isHiding = false
				this@BubbleAnimator.animator = null
			}
		}
	}
}
