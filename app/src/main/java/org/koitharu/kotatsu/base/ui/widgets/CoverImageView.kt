package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import org.koitharu.kotatsu.R
import kotlin.math.roundToInt


class CoverImageView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

	private var orientation: Int = HORIZONTAL

	init {
		context.withStyledAttributes(attrs, R.styleable.CoverImageView, defStyleAttr) {
			orientation = getInt(R.styleable.CoverImageView_android_orientation, orientation)
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		val desiredWidth: Int
		val desiredHeight: Int
		if (orientation == VERTICAL) {
			desiredHeight = measuredHeight
			desiredWidth = (desiredHeight * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).roundToInt()
		} else {
			desiredWidth = measuredWidth
			desiredHeight = (desiredWidth * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).roundToInt()
		}
		setMeasuredDimension(desiredWidth, desiredHeight)
	}

	companion object {

		const val VERTICAL = LinearLayout.VERTICAL
		const val HORIZONTAL = LinearLayout.HORIZONTAL

		private const val ASPECT_RATIO_HEIGHT = 18f
		private const val ASPECT_RATIO_WIDTH = 13f
	}
}