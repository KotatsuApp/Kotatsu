package org.koitharu.kotatsu.core.ui.list.fastscroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.animatorDurationScale

class ScrollbarAnimator(
	private val scrollbar: View,
	private val scrollbarPaddingEnd: Float,
) {

	private val animationDuration = (
		scrollbar.resources.getInteger(R.integer.config_defaultAnimTime) *
			scrollbar.context.animatorDurationScale
		).toLong()
	private var animator: ViewPropertyAnimator? = null
	private var isHiding = false

	fun show() {
		if (scrollbar.isVisible && !isHiding) {
			return
		}
		isHiding = false
		animator?.cancel()
		scrollbar.translationX = scrollbarPaddingEnd
		scrollbar.isVisible = true
		animator = scrollbar
			.animate()
			.translationX(0f)
			.alpha(1f)
			.setListener(null)
			.setDuration(animationDuration)
	}

	fun hide() {
		if (!scrollbar.isVisible || isHiding) {
			return
		}
		animator?.cancel()
		isHiding = true
		animator = scrollbar.animate().apply {
			translationX(scrollbarPaddingEnd)
			alpha(0f)
			duration = animationDuration
			setListener(HideListener(this))
		}
	}

	private inner class HideListener(
		private val viewPropertyAnimator: ViewPropertyAnimator,
	) : AnimatorListenerAdapter() {

		private var isCancelled = false

		override fun onAnimationCancel(animation: Animator) {
			super.onAnimationCancel(animation)
			isCancelled = true
		}

		override fun onAnimationEnd(animation: Animator) {
			super.onAnimationEnd(animation)
			if (!isCancelled && this@ScrollbarAnimator.animator === viewPropertyAnimator) {
				scrollbar.isInvisible = true
				isHiding = false
				this@ScrollbarAnimator.animator = null
			}
		}
	}
}
