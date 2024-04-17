package org.koitharu.kotatsu.core.ui.image

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.ColorUtils
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.animation.ArgbEvaluatorCompat
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import kotlin.math.abs
import com.google.android.material.R as materialR

class AnimatedPlaceholderDrawable(context: Context) : Drawable(), Animatable, TimeAnimator.TimeListener {

	private val colorLow = context.getThemeColor(materialR.attr.colorSurfaceContainerLow)
	private val colorHigh = context.getThemeColor(materialR.attr.colorSurfaceContainerHigh)
	private var currentColor: Int = colorLow
	private var alpha: Int = 255
	private val interpolator = FastOutSlowInInterpolator()
	private val period = 2000
	private val timeAnimator = TimeAnimator()

	init {
		timeAnimator.setTimeListener(this)
		updateColor()
	}

	override fun draw(canvas: Canvas) {
		if (!isRunning) {
			updateColor()
			start()
		}
		canvas.drawColor(currentColor)
	}

	override fun setAlpha(alpha: Int) {
		this.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		throw UnsupportedOperationException("ColorFilter is not supported by PlaceholderDrawable")
	}

	@Deprecated("Deprecated in Java")
	override fun getOpacity(): Int = PixelFormat.OPAQUE

	override fun getAlpha(): Int = alpha

	override fun onTimeUpdate(animation: TimeAnimator?, totalTime: Long, deltaTime: Long) {
		if (callback != null) {
			updateColor()
			invalidateSelf()
		}
	}

	override fun start() {
		timeAnimator.start()
	}

	override fun stop() {
		timeAnimator.cancel()
	}

	override fun isRunning(): Boolean = timeAnimator.isStarted

	private fun updateColor() {
		val ph = period / 2
		val fraction = abs((System.currentTimeMillis() % period) - ph) / ph.toFloat()
		var color = ArgbEvaluatorCompat.getInstance()
			.evaluate(interpolator.getInterpolation(fraction), colorLow, colorHigh)
		if (alpha != 255) {
			color = ColorUtils.setAlphaComponent(color, alpha)
		}
		currentColor = color
	}
}
