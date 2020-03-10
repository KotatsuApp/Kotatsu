package org.koitharu.kotatsu.ui.reader.wetoon

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.sign

class WebtoonRecyclerView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

	override fun dispatchNestedPreScroll(
		dx: Int,
		dy: Int,
		consumed: IntArray?,
		offsetInWindow: IntArray?,
		type: Int
	): Boolean {
		val consumedY = consumeVerticalScroll(dy)
		val superRes = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
		consumed?.set(1, consumed[1] + consumedY)
		return superRes || consumedY != 0
	}

	override fun dispatchNestedPreScroll(
		dx: Int,
		dy: Int,
		consumed: IntArray?,
		offsetInWindow: IntArray?
	): Boolean {
		val consumedY = consumeVerticalScroll(dy)
		val superRes = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
		consumed?.set(1, consumed[1] + consumedY)
		return superRes || consumedY != 0
	}

	private fun consumeVerticalScroll(dy: Int): Int {
		val child = when {
			dy > 0 -> children.firstOrNull { it is WebtoonFrameLayout }
			dy < 0 -> children.lastOrNull { it is WebtoonFrameLayout }
			else -> null
		} ?: return 0
		var scrollY = dy
		scrollY -= (child as WebtoonFrameLayout).dispatchVerticalScroll(scrollY)
		return dy - scrollY
	}
}