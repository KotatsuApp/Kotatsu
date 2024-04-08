package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import androidx.annotation.AttrRes
import androidx.core.content.withStyledAttributes
import com.google.android.material.imageview.ShapeableImageView
import org.koitharu.kotatsu.R
import kotlin.math.roundToInt

private const val ASPECT_RATIO_HEIGHT = 3f
private const val ASPECT_RATIO_WIDTH = 2f

class CoverImageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : ShapeableImageView(context, attrs, defStyleAttr) {

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
}
