package org.koitharu.kotatsu.base.ui.list.fastscroll

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R

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

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		fastScroller.attachRecyclerView(this)
	}

	override fun onDetachedFromWindow() {
		fastScroller.detachRecyclerView()
		super.onDetachedFromWindow()
	}
}