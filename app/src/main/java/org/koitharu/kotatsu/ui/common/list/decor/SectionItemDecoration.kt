package org.koitharu.kotatsu.ui.common.list.decor

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.inflate
import kotlin.math.max

/**
 * https://github.com/paetztm/recycler_view_headers
 */
class SectionItemDecoration(
	private val isSticky: Boolean,
	private val callback: Callback
) : RecyclerView.ItemDecoration() {

	private var headerView: TextView? = null
	private var headerOffset: Int = 0

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		if (headerOffset == 0) {
			headerOffset = parent.resources.getDimensionPixelSize(R.dimen.header_height)
		}
		val pos = parent.getChildAdapterPosition(view)
		outRect.set(0, if (callback.isSection(pos)) headerOffset else 0, 0, 0)
	}

	override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		super.onDrawOver(c, parent, state)
		val textView = headerView ?: parent.inflate<TextView>(R.layout.item_header).also {
			headerView = it
		}
		fixLayoutSize(textView, parent)

		for (child in parent.children) {
			val pos = parent.getChildAdapterPosition(child)
			if (callback.isSection(pos)) {
				textView.text = callback.getSectionTitle(pos) ?: continue
				c.save()
				if (isSticky) {
					c.translate(
						0f,
						max(0f, (child.top - textView.height).toFloat())
					)
				} else {
					c.translate(
						0f,
						(child.top - textView.height).toFloat()
					)
				}
				textView.draw(c)
				c.restore()
			}
		}
	}

	/**
	 * Measures the header view to make sure its size is greater than 0 and will be drawn
	 * https://yoda.entelect.co.za/view/9627/how-to-android-recyclerview-item-decorations
	 */
	private fun fixLayoutSize(view: View, parent: ViewGroup) {
		val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
		val heightSpec =
			View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

		val childWidth = ViewGroup.getChildMeasureSpec(
			widthSpec,
			parent.paddingLeft + parent.paddingRight,
			view.layoutParams.width
		)
		val childHeight = ViewGroup.getChildMeasureSpec(
			heightSpec,
			parent.paddingTop + parent.paddingBottom,
			view.layoutParams.height
		)
		view.measure(childWidth, childHeight)
		view.layout(0, 0, view.measuredWidth, view.measuredHeight)
	}

	interface Callback {

		fun isSection(position: Int): Boolean

		fun getSectionTitle(position: Int): CharSequence?
	}
}