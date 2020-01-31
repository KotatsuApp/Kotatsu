package org.koitharu.kotatsu.ui.common.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView


class CoverImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            val originalWidth = MeasureSpec.getSize(widthMeasureSpec)
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(originalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(
                    (originalWidth * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).toInt(),
                    MeasureSpec.EXACTLY
                )
            )
        } else {
            val originalHeight = MeasureSpec.getSize(heightMeasureSpec)
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(
                    (originalHeight * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).toInt(),
                    MeasureSpec.EXACTLY
                ),
                MeasureSpec.makeMeasureSpec(originalHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    private companion object {

        const val ASPECT_RATIO_HEIGHT = 18f
        const val ASPECT_RATIO_WIDTH = 13f
    }
}