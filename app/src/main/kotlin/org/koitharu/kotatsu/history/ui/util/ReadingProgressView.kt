package org.koitharu.kotatsu.history.ui.util

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.history.data.PROGRESS_NONE

class ReadingProgressView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

	private var percentAnimator: ValueAnimator? = null
	private val animationDuration = context.getAnimationDuration(android.R.integer.config_shortAnimTime)

	@StyleRes
	private val drawableStyle: Int

	var percent: Float
		get() = peekProgressDrawable()?.progress ?: PROGRESS_NONE
		set(value) {
			cancelAnimation()
			getProgressDrawable().progress = value
		}

	init {
		val ta = context.obtainStyledAttributes(attrs, R.styleable.ReadingProgressView, defStyleAttr, 0)
		drawableStyle = ta.getResourceId(R.styleable.ReadingProgressView_progressStyle, R.style.ProgressDrawable)
		ta.recycle()
		outlineProvider = OutlineProvider()
		if (isInEditMode) {
			percent = 0.27f
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		percentAnimator?.run {
			if (isRunning) end()
		}
		percentAnimator = null
	}

	override fun onAnimationUpdate(animation: ValueAnimator) {
		val p = animation.animatedValue as Float
		getProgressDrawable().progress = p
	}

	override fun onAnimationStart(animation: Animator) = Unit

	override fun onAnimationEnd(animation: Animator) {
		if (percentAnimator === animation) {
			percentAnimator = null
		}
	}

	override fun onAnimationCancel(animation: Animator) = Unit

	override fun onAnimationRepeat(animation: Animator) = Unit

	fun setPercent(value: Float, animate: Boolean) {
		val currentDrawable = peekProgressDrawable()
		if (!animate || currentDrawable == null || value == PROGRESS_NONE) {
			percent = value
			return
		}
		percentAnimator?.cancel()
		percentAnimator = ValueAnimator.ofFloat(
			currentDrawable.progress.coerceAtLeast(0f),
			value,
		).apply {
			duration = animationDuration
			interpolator = AccelerateDecelerateInterpolator()
			addUpdateListener(this@ReadingProgressView)
			addListener(this@ReadingProgressView)
			start()
		}
	}

	private fun cancelAnimation() {
		percentAnimator?.cancel()
		percentAnimator = null
	}

	private fun peekProgressDrawable(): ReadingProgressDrawable? {
		return background as? ReadingProgressDrawable
	}

	private fun getProgressDrawable(): ReadingProgressDrawable {
		var d = peekProgressDrawable()
		if (d != null) {
			return d
		}
		d = ReadingProgressDrawable(context, drawableStyle)
		background = d
		return d
	}

	private class OutlineProvider : ViewOutlineProvider() {

		override fun getOutline(view: View, outline: Outline) {
			outline.setOval(0, 0, view.width, view.height)
		}
	}
}
