package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.resolveAdjustedSize


class CoverImageView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

	private var orientation: Int = HORIZONTAL

	init {
		context.withStyledAttributes(attrs, R.styleable.CoverImageView, defStyleAttr) {
			orientation = getInt(R.styleable.CoverImageView_android_orientation, HORIZONTAL)
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val w: Int
		val h: Int
		if (orientation == VERTICAL) {
			val desiredHeight = (drawable?.intrinsicHeight?.coerceAtLeast(0) ?: 0) +
					paddingTop + paddingBottom
			h = resolveAdjustedSize(
				desiredHeight.coerceAtLeast(suggestedMinimumHeight),
				maxHeight,
				heightMeasureSpec
			)
			val desiredWidth =
				(h * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).toInt() + paddingLeft + paddingRight
			w = resolveAdjustedSize(
				desiredWidth.coerceAtLeast(suggestedMinimumWidth),
				maxWidth,
				widthMeasureSpec
			)
		} else {
			val desiredWidth = (drawable?.intrinsicWidth?.coerceAtLeast(0) ?: 0) +
					paddingLeft + paddingRight
			w = resolveAdjustedSize(
				desiredWidth.coerceAtLeast(suggestedMinimumWidth),
				maxWidth,
				widthMeasureSpec
			)
			val desiredHeight =
				(w * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).toInt() + paddingTop + paddingBottom
			h = resolveAdjustedSize(
				desiredHeight.coerceAtLeast(suggestedMinimumHeight),
				maxHeight,
				heightMeasureSpec
			)
		}
		val widthSize = resolveSizeAndState(w, widthMeasureSpec, 0)
		val heightSize = resolveSizeAndState(h, heightMeasureSpec, 0)
		setMeasuredDimension(widthSize, heightSize)
	}

	companion object {

		const val VERTICAL = LinearLayout.VERTICAL
		const val HORIZONTAL = LinearLayout.HORIZONTAL

		private const val ASPECT_RATIO_HEIGHT = 18f
		private const val ASPECT_RATIO_WIDTH = 13f
	}
}