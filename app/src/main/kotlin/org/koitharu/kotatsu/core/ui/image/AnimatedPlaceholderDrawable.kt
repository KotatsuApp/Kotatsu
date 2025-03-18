package org.koitharu.kotatsu.core.ui.image

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.animation.ArgbEvaluatorCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import kotlin.math.abs
import com.google.android.material.R as materialR

class AnimatedPlaceholderDrawable(context: Context) : Drawable(), Animatable, TimeAnimator.TimeListener {

	private val colorLow = context.getThemeColor(materialR.attr.colorSurfaceContainerLowest)
	private val colorHigh = context.getThemeColor(materialR.attr.colorSurfaceContainerHighest)
	private var currentColor: Int = colorLow
	private val interpolator = FastOutSlowInInterpolator()
	private val period = context.getAnimationDuration(R.integer.config_longAnimTime) * 2
	private val timeAnimator = TimeAnimator()
	private var currentAlpha: Int = 255

	init {
		timeAnimator.setTimeListener(this)
		updateColor()
	}

	override fun draw(canvas: Canvas) {
		if (!isRunning && period > 0) {
			updateColor()
			start()
		}
		canvas.drawColor(currentColor)
	}

	override fun setAlpha(alpha: Int) {
		currentAlpha = alpha
		updateColor()
	}

	@Suppress("DeprecatedCallableAddReplaceWith")
	@Deprecated("Deprecated in Java")
	override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

	override fun getAlpha(): Int = currentAlpha

	override fun setColorFilter(colorFilter: ColorFilter?) = Unit

	override fun onTimeUpdate(animation: TimeAnimator?, totalTime: Long, deltaTime: Long) {
		callback?.also {
			updateColor()
			it.invalidateDrawable(this)
		} ?: stop()
	}

	override fun start() {
		timeAnimator.start()
	}

	override fun stop() {
		timeAnimator.end()
	}

	override fun isRunning(): Boolean = timeAnimator.isStarted

	private fun updateColor() {
		if (period <= 0f) {
			return
		}
		val ph = period / 2
		val fraction = abs((System.currentTimeMillis() % period) - ph) / ph.toFloat()
		currentColor = ColorUtils.setAlphaComponent(
			ArgbEvaluatorCompat.getInstance()
				.evaluate(interpolator.getInterpolation(fraction), colorLow, colorHigh),
			currentAlpha
		)
	}
}
