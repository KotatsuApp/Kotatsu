package org.koitharu.kotatsu.base.ui.list.fastscroll

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.parents

class FastScrollRecyclerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = androidx.recyclerview.R.attr.recyclerViewStyle,
) : RecyclerView(context, attrs, defStyleAttr) {

	val fastScroller = FastScroller(context, attrs)

	init {
		fastScroller.id = R.id.fast_scroller
		fastScroller.layoutParams = ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.MATCH_PARENT,
		)
	}

	override fun setAdapter(adapter: Adapter<*>?) {
		super.setAdapter(adapter)
		fastScroller.setSectionIndexer(adapter as? FastScroller.SectionIndexer)
	}

	override fun setVisibility(visibility: Int) {
		super.setVisibility(visibility)
		fastScroller.visibility = visibility
	}

	fun setFastScrollListener(fastScrollListener: FastScroller.FastScrollListener?) =
		fastScroller.setFastScrollListener(fastScrollListener)

	fun setFastScrollEnabled(enabled: Boolean) {
		fastScroller.isEnabled = enabled
	}

	fun setHideScrollbar(hideScrollbar: Boolean) = fastScroller.setHideScrollbar(hideScrollbar)

	fun setTrackVisible(visible: Boolean) = fastScroller.setTrackVisible(visible)

	fun setTrackColor(@ColorInt color: Int) = fastScroller.setTrackColor(color)

	fun setHandleColor(@ColorInt color: Int) = fastScroller.setHandleColor(color)

	@JvmOverloads
	fun setBubbleVisible(visible: Boolean, always: Boolean = false) = fastScroller.setBubbleVisible(visible, always)

	fun setBubbleColor(@ColorInt color: Int) = fastScroller.setBubbleColor(color)

	fun setBubbleTextColor(@ColorInt color: Int) = fastScroller.setBubbleTextColor(color)

	fun setBubbleTextSize(size: Int) = fastScroller.setBubbleTextSize(size)

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		fastScroller.attachRecyclerView(this)
		for (p in parents) {
			if (p is SwipeRefreshLayout) {
				fastScroller.setSwipeRefreshLayout(p)
				return
			}
		}
	}

	override fun onDetachedFromWindow() {
		fastScroller.detachRecyclerView()
		fastScroller.setSwipeRefreshLayout(null)
		super.onDetachedFromWindow()
	}
}