package org.koitharu.kotatsu.core.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.widget.TextViewCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.getThemeColorStateList
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import com.google.android.material.R as materialR

class ProgressButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : LinearLayoutCompat(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

	private val textViewTitle = TextView(context)
	private val textViewSubtitle = TextView(context)
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	private var progress = 0f
	private var targetProgress = 0f
	private var colorBase: ColorStateList = ColorStateList.valueOf(Color.TRANSPARENT)
	private var colorProgress: ColorStateList = ColorStateList.valueOf(Color.TRANSPARENT)
	private var progressAnimator: ValueAnimator? = null

	private var colorBaseCurrent = colorProgress.defaultColor
	private var colorProgressCurrent = colorProgress.defaultColor

	var title: CharSequence?
		get() = textViewTitle.textAndVisible
		set(value) {
			textViewTitle.textAndVisible = value
		}

	var subtitle: CharSequence?
		get() = textViewSubtitle.textAndVisible
		set(value) {
			textViewSubtitle.textAndVisible = value
		}

	init {
		orientation = VERTICAL
		outlineProvider = OutlineProvider()
		clipToOutline = true

		context.withStyledAttributes(attrs, R.styleable.ProgressButton, defStyleAttr) {
			val textAppearanceFallback = androidx.appcompat.R.style.TextAppearance_AppCompat
			TextViewCompat.setTextAppearance(
				textViewTitle,
				getResourceId(R.styleable.ProgressButton_titleTextAppearance, textAppearanceFallback),
			)
			TextViewCompat.setTextAppearance(
				textViewSubtitle,
				getResourceId(R.styleable.ProgressButton_subtitleTextAppearance, textAppearanceFallback),
			)
			textViewTitle.text = getText(R.styleable.ProgressButton_title)
			textViewSubtitle.text = getText(R.styleable.ProgressButton_subtitle)
			colorBase = getColorStateList(R.styleable.ProgressButton_baseColor)
				?: context.getThemeColorStateList(materialR.attr.colorPrimaryContainer) ?: colorBase
			colorProgress = getColorStateList(R.styleable.ProgressButton_progressColor)
				?: context.getThemeColorStateList(materialR.attr.colorPrimary) ?: colorProgress
			getColorStateList(R.styleable.ProgressButton_android_textColor)?.let { colorText ->
				textViewTitle.setTextColor(colorText)
				textViewSubtitle.setTextColor(colorText)
			}
			progress = getInt(R.styleable.ProgressButton_android_progress, 0).toFloat() /
				getInt(R.styleable.ProgressButton_android_max, 100).toFloat()
		}

		addView(textViewTitle, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
		addView(
			textViewSubtitle,
			LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also { lp ->
				lp.topMargin = context.resources.resolveDp(2)
			},
		)

		paint.style = Paint.Style.FILL
		applyGravity()
		setWillNotDraw(false)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		canvas.drawColor(colorBaseCurrent)
		if (progress > 0f) {
			canvas.drawRect(0f, 0f, width * progress, height.toFloat(), paint)
		}
	}

	override fun drawableStateChanged() {
		super.drawableStateChanged()
		val state = drawableState
		colorBaseCurrent = colorBase.getColorForState(state, colorBase.defaultColor)
		colorProgressCurrent = colorProgress.getColorForState(state, colorProgress.defaultColor)
		colorProgressCurrent = ColorUtils.setAlphaComponent(colorProgressCurrent, 84 /* 255 * 0.33F */)
		paint.color = colorProgressCurrent
	}

	override fun setGravity(gravity: Int) {
		super.setGravity(gravity)
		if (childCount != 0) {
			applyGravity()
		}
	}

	override fun setEnabled(enabled: Boolean) {
		super.setEnabled(enabled)
		children.forEach { it.isEnabled = enabled }
	}

	override fun onAnimationUpdate(animation: ValueAnimator) {
		if (animation === progressAnimator) {
			progress = animation.animatedValue as Float
			invalidate()
		}
	}

	fun setTitle(@StringRes titleResId: Int) {
		textViewTitle.setTextAndVisible(titleResId)
	}

	fun setSubtitle(@StringRes titleResId: Int) {
		textViewSubtitle.setTextAndVisible(titleResId)
	}

	fun setProgress(value: Float, animate: Boolean) {
		val prevAnimator = progressAnimator
		if (animate && context.isAnimationsEnabled) {
			if (value == targetProgress) {
				return
			}
			targetProgress = value
			progressAnimator = ValueAnimator.ofFloat(progress, value).apply {
				duration = context.getAnimationDuration(android.R.integer.config_mediumAnimTime)
				interpolator = AccelerateDecelerateInterpolator()
				addUpdateListener(this@ProgressButton)
			}
			progressAnimator?.start()
		} else {
			progressAnimator = null
			progress = value
			targetProgress = value
			invalidate()
		}
		prevAnimator?.cancel()
	}

	private fun applyGravity() {
		val value = (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) or Gravity.CENTER_VERTICAL
		textViewTitle.gravity = value
		textViewSubtitle.gravity = value
	}

	private class OutlineProvider : ViewOutlineProvider() {

		override fun getOutline(view: View, outline: Outline) {
			outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
		}
	}
}
