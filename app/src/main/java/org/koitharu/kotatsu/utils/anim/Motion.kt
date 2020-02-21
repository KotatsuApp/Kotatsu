package org.koitharu.kotatsu.utils.anim

import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator
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

	object CheckEffect : Motion() {
		override fun reset(v: View) {
			v.scaleX = 1f
			v.scaleY = 1f
		}

		override fun hideView(v: View) {
			v.scaleX = 0f
			v.scaleY = 0f
		}

		override fun hide(v: View, anim: ViewPropertyAnimator) {
			anim.scaleX(0f)
			anim.scaleY(0f)
			anim.interpolator = AccelerateDecelerateInterpolator()
		}

		override fun show(v: View, anim: ViewPropertyAnimator) {
			anim.scaleY(1f)
			anim.scaleX(1f)
			anim.interpolator = AccelerateDecelerateInterpolator()
		}
	}
}