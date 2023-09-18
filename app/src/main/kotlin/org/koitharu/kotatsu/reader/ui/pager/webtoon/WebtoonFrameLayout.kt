package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import org.koitharu.kotatsu.R

class WebtoonFrameLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

	val target: WebtoonImageView by lazy(LazyThreadSafetyMode.NONE) {
		findViewById(R.id.ssiv)
	}

	fun dispatchVerticalScroll(dy: Int): Int {
		if (dy == 0) {
			return 0
		}
		val oldScroll = target.getScroll()
		target.scrollBy(dy)
		return target.getScroll() - oldScroll
	}
}
