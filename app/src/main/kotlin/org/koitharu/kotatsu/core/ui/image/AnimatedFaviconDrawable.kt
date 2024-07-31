package org.koitharu.kotatsu.core.ui.image

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import androidx.annotation.StyleRes
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.color.MaterialColors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.KotatsuColors
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import kotlin.math.abs

class AnimatedFaviconDrawable(
	context: Context,
	@StyleRes styleResId: Int,
	name: String,
) : FaviconDrawable(context, styleResId, name), Animatable, TimeAnimator.TimeListener {

	private val interpolator = FastOutSlowInInterpolator()
	private val period = context.getAnimationDuration(R.integer.config_longAnimTime) * 2
	private val timeAnimator = TimeAnimator()

	private val colorHigh = MaterialColors.harmonize(KotatsuColors.random(name), colorBackground)
	private val colorLow = ArgbEvaluatorCompat.getInstance().evaluate(0.3f, colorHigh, colorBackground)

	init {
		timeAnimator.setTimeListener(this)
		updateColor()
	}

	override fun draw(canvas: Canvas) {
		if (!isRunning && period > 0) {
			updateColor()
			start()
		}
		super.draw(canvas)
	}

	override fun setAlpha(alpha: Int) = Unit

	override fun getAlpha(): Int = 255

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
		colorForeground = ArgbEvaluatorCompat.getInstance()
			.evaluate(interpolator.getInterpolation(fraction), colorLow, colorHigh)
	}
}
