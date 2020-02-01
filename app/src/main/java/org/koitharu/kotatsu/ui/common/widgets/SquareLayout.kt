package org.koitharu.kotatsu.ui.common.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class SquareLayout @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, widthMeasureSpec)
	}
}