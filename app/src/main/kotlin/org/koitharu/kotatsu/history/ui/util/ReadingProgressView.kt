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
import org.koitharu.kotatsu.core.prefs.ProgressIndicatorMode.CHAPTERS_LEFT
import org.koitharu.kotatsu.core.prefs.ProgressIndicatorMode.CHAPTERS_READ
import org.koitharu.kotatsu.core.prefs.ProgressIndicatorMode.NONE
import org.koitharu.kotatsu.core.prefs.ProgressIndicatorMode.PERCENT_LEFT
import org.koitharu.kotatsu.core.prefs.ProgressIndicatorMode.PERCENT_READ
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.list.domain.ReadingProgress
import org.koitharu.kotatsu.list.domain.ReadingProgress.Companion.PROGRESS_NONE

class ReadingProgressView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

	private val percentPattern = context.getString(R.string.percent_string_pattern)
	private var percentAnimator: ValueAnimator? = null
	private val animationDuration = context.getAnimationDuration(android.R.integer.config_shortAnimTime)

	@StyleRes
	private val drawableStyle: Int

	var progress: ReadingProgress? = null
		set(value) {
			field = value
			cancelAnimation()
			getProgressDrawable().also {
				it.percent = value?.percent ?: PROGRESS_NONE
				it.text = when (value?.mode) {
					null,
					NONE -> ""

					PERCENT_READ -> percentPattern.format((value.percent * 100f).toInt().toString())
					PERCENT_LEFT -> "-" + percentPattern.format((value.percentLeft * 100f).toInt().toString())

					CHAPTERS_READ -> value.chapters.toString()
					CHAPTERS_LEFT -> "-" + value.chaptersLeft.toString()
				}
			}
		}

	init {
		val ta = context.obtainStyledAttributes(attrs, R.styleable.ReadingProgressView, defStyleAttr, 0)
		drawableStyle = ta.getResourceId(R.styleable.ReadingProgressView_progressStyle, R.style.ProgressDrawable)
		ta.recycle()
		outlineProvider = OutlineProvider()
		if (isInEditMode) {
			progress = ReadingProgress(
				percent = 0.27f,
				totalChapters = 20,
				mode = PERCENT_READ,
			)
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
		getProgressDrawable().percent = p
	}

	override fun onAnimationStart(animation: Animator) = Unit

	override fun onAnimationEnd(animation: Animator) {
		if (percentAnimator === animation) {
			percentAnimator = null
		}
	}

	override fun onAnimationCancel(animation: Animator) = Unit

	override fun onAnimationRepeat(animation: Animator) = Unit

	fun setProgress(percent: Float, animate: Boolean) {
		setProgress(
			value = ReadingProgress(percent, 1, PERCENT_READ),
			animate = animate,
		)
	}

	fun setProgress(value: ReadingProgress?, animate: Boolean) {
		val currentDrawable = peekProgressDrawable()
		if (!animate || currentDrawable == null || value == null) {
			progress = value
			return
		}
		percentAnimator?.cancel()
		val currentPercent = currentDrawable.percent.coerceAtLeast(0f)
		progress = value.copy(percent = currentPercent)
		percentAnimator = ValueAnimator.ofFloat(
			currentDrawable.percent.coerceAtLeast(0f),
			value.percent,
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
