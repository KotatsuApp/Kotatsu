package org.koitharu.kotatsu.core.ui.list

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams

class FitHeightLinearLayoutManager : LinearLayoutManager {

	constructor(context: Context) : super(context)
	constructor(
		context: Context,
		@RecyclerView.Orientation orientation: Int,
		reverseLayout: Boolean,
	) : super(context, orientation, reverseLayout)

	constructor(
		context: Context,
		attrs: AttributeSet?,
		@AttrRes defStyleAttr: Int,
		@StyleRes defStyleRes: Int,
	) : super(context, attrs, defStyleAttr, defStyleRes)

	override fun layoutDecoratedWithMargins(child: View, left: Int, top: Int, right: Int, bottom: Int) {
		if (orientation == RecyclerView.VERTICAL && child.layoutParams.height == LayoutParams.MATCH_PARENT) {
			val parentBottom = height - paddingBottom
			val offset = parentBottom - bottom
			super.layoutDecoratedWithMargins(child, left, top, right, bottom + offset)
		} else {
			super.layoutDecoratedWithMargins(child, left, top, right, bottom)
		}
	}
}
