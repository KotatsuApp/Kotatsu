package org.koitharu.kotatsu.ui.reader.wetoon

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class WebtoonImageView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) :
	SubsamplingScaleImageView(context, attr) {

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent) = false

}