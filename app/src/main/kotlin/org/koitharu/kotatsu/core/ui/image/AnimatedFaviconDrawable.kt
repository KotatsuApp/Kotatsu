package org.koitharu.kotatsu.core.ui.image

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import androidx.annotation.StyleRes
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import coil3.Image
import coil3.asImage
import coil3.getExtra
import coil3.request.ImageRequest
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.color.MaterialColors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.mangaSourceKey
import kotlin.math.abs

class AnimatedFaviconDrawable(
	context: Context,
	@StyleRes styleResId: Int,
	name: String,
) : FaviconDrawable(context, styleResId, name), Animatable, TimeAnimator.TimeListener {

	private val interpolator = FastOutSlowInInterpolator()
	private val period = context.getAnimationDuration(R.integer.config_longAnimTime) * 2
	private val timeAnimator = TimeAnimator()

	private var colorHigh = MaterialColors.harmonize(colorForeground, currentBackgroundColor)
	private var colorLow = ArgbEvaluatorCompat.getInstance().evaluate(0.3f, colorHigh, currentBackgroundColor)

	init {
		timeAnimator.setTimeListener(this)
		onStateChange(state)
	}

	override fun draw(canvas: Canvas) {
		if (!isRunning && period > 0) {
			updateColor()
			start()
		}
		super.draw(canvas)
	}

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

	override fun onStateChange(state: IntArray): Boolean {
		val res = super.onStateChange(state)
		colorHigh = MaterialColors.harmonize(currentForegroundColor, currentBackgroundColor)
		colorLow = ArgbEvaluatorCompat.getInstance().evaluate(0.3f, colorHigh, currentBackgroundColor)
		updateColor()
		return res
	}

	private fun updateColor() {
		if (period <= 0f) {
			return
		}
		val ph = period / 2
		val fraction = abs((System.currentTimeMillis() % period) - ph) / ph.toFloat()
		currentForegroundColor = ArgbEvaluatorCompat.getInstance()
			.evaluate(interpolator.getInterpolation(fraction), colorLow, colorHigh)
	}

	class Factory(
		@StyleRes private val styleResId: Int,
	) : ((ImageRequest) -> Image?) {

		override fun invoke(request: ImageRequest): Image? {
			val source = request.getExtra(mangaSourceKey) ?: return null
			val context = request.context
			val title = source.getTitle(context)
			return AnimatedFaviconDrawable(context, styleResId, title).asImage()
		}
	}
}
