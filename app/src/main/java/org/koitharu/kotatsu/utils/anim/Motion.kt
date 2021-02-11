package org.koitharu.kotatsu.utils.anim

import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import org.koitharu.kotatsu.utils.ext.measureHeight

sealed class Motion {

	abstract fun reset(v: View)

	abstract fun hideView(v: View)

	abstract fun hide(v: View, anim: ViewPropertyAnimator)

	abstract fun show(v: View, anim: ViewPropertyAnimator)

	object CrossFade : Motion() {
		override fun reset(v: View) {
			v.alpha = 1f
		}

		override fun hideView(v: View) {
			v.alpha = 0f
		}

		override fun hide(v: View, anim: ViewPropertyAnimator) {
			anim.alpha(0f)
		}

		override fun show(v: View, anim: ViewPropertyAnimator) {
			anim.alpha(1f)
		}
	}

	object SlideBottom : Motion() {
		override fun reset(v: View) {
			v.translationY = 0f
		}

		override fun hideView(v: View) {
			v.translationY = v.measureHeight().toFloat()
		}

		override fun hide(v: View, anim: ViewPropertyAnimator) {
			anim.translationY(v.measureHeight().toFloat())
		}

		override fun show(v: View, anim: ViewPropertyAnimator) {
			anim.translationY(0f)
		}
	}

	object SlideTop : Motion() {
		override fun reset(v: View) {
			v.translationY = 0f
		}

		override fun hideView(v: View) {
			v.translationY = -v.measureHeight().toFloat()
		}

		override fun hide(v: View, anim: ViewPropertyAnimator) {
			anim.translationY(-v.measureHeight().toFloat())
		}

		override fun show(v: View, anim: ViewPropertyAnimator) {
			anim.translationY(0f)
		}
	}

	object Toast : Motion() {
		override fun reset(v: View) {
			v.alpha = 1f
		}

		override fun hideView(v: View) {
			v.alpha = 0f
		}

		override fun hide(v: View, anim: ViewPropertyAnimator) {
			anim.alpha(0f)
			anim.translationY(v.measureHeight().toFloat())
			anim.interpolator = AccelerateInterpolator()
		}

		override fun show(v: View, anim: ViewPropertyAnimator) {
			anim.alpha(1f)
			anim.translationY(0f)
			anim.interpolator = DecelerateInterpolator()
		}
	}
}