package org.koitharu.kotatsu.reader.ui.pager.doublepage

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DoublePageLayoutManager(
	context: Context,
	attrs: AttributeSet?,
	defStyleAttr: Int,
	defStyleRes: Int,
) : LinearLayoutManager(context, attrs, defStyleAttr, defStyleRes) {

	override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
		lp?.width = width / 2
		return super.checkLayoutParams(lp)
	}
}
