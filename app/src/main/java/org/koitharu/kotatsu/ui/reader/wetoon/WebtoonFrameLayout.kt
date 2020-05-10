package org.koitharu.kotatsu.ui.reader.wetoon

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import org.koitharu.kotatsu.R

class WebtoonFrameLayout @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private val target by lazy {
		findViewById<WebtoonImageView>(R.id.ssiv)
	}

	fun dispatchVerticalScroll(dy: Int) = target.dispatchVerticalScroll(dy)
}