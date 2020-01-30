package org.koitharu.kotatsu.ui.common.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView


class CoverImageView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {


	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val originalWidth = MeasureSpec.getSize(widthMeasureSpec)
		val calculatedHeight: Int = (originalWidth * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).toInt()

		super.onMeasure(
			MeasureSpec.makeMeasureSpec(originalWidth, MeasureSpec.EXACTLY),
			MeasureSpec.makeMeasureSpec(calculatedHeight, MeasureSpec.EXACTLY)
		)
	}

	private companion object {

		const val ASPECT_RATIO_HEIGHT = 18f
		const val ASPECT_RATIO_WIDTH = 13f
	}
}