package org.koitharu.kotatsu.ui.common.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import moxy.MvpFacade.init
import org.koitharu.kotatsu.R


class CoverImageView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

	private var orientation: Int = HORIZONTAL

	init {
		context.theme.obtainStyledAttributes(attrs, R.styleable.CoverImageView, defStyleAttr, 0)
			.use {
				orientation = it.getInt(R.styleable.CoverImageView_android_orientation, HORIZONTAL)
			}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		if (orientation == VERTICAL) {
			val originalHeight = MeasureSpec.getSize(heightMeasureSpec)
			super.onMeasure(
				MeasureSpec.makeMeasureSpec(
					(originalHeight * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).toInt(),
					MeasureSpec.EXACTLY
				),
				MeasureSpec.makeMeasureSpec(originalHeight, MeasureSpec.EXACTLY)
			)
		} else {
			val originalWidth = MeasureSpec.getSize(widthMeasureSpec)
			super.onMeasure(
				MeasureSpec.makeMeasureSpec(originalWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(
					(originalWidth * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).toInt(),
					MeasureSpec.EXACTLY
				)
			)
		}
	}

	companion object {

		const val VERTICAL = LinearLayout.VERTICAL
		const val HORIZONTAL = LinearLayout.HORIZONTAL

		private const val ASPECT_RATIO_HEIGHT = 18f
		private const val ASPECT_RATIO_WIDTH = 13f
	}
}