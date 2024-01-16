package org.koitharu.kotatsu.core.ui.list.fastscroll

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.ancestors
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.koitharu.kotatsu.R

class FastScrollRecyclerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = androidx.recyclerview.R.attr.recyclerViewStyle,
) : RecyclerView(context, attrs, defStyleAttr) {

	val fastScroller = FastScroller(context, attrs)
	private var applyViewPager2Fix = false

	var isFastScrollerEnabled: Boolean = true
		set(value) {
			field = value
			fastScroller.isVisible = value && isVisible
		}

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
		fastScroller.visibility = if (isFastScrollerEnabled) visibility else GONE
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		fastScroller.attachRecyclerView(this)
		applyViewPager2Fix = ancestors.any { it is ViewPager2 } == true
	}

	override fun onDetachedFromWindow() {
		fastScroller.detachRecyclerView()
		super.onDetachedFromWindow()
		applyViewPager2Fix = false
	}

	override fun isLayoutRequested(): Boolean {
		return if (applyViewPager2Fix) false else super.isLayoutRequested()
	}

	override fun requestLayout() {
		super.requestLayout()
		if (applyViewPager2Fix && parent?.isLayoutRequested == true) {
			parent?.requestLayout()
		}
	}
}
