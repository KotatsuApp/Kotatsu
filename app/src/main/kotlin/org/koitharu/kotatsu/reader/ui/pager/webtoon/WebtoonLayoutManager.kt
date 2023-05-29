package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.sign

@Suppress("unused")
class WebtoonLayoutManager : LinearLayoutManager {

	private var scrollDirection: Int = 0

	constructor(context: Context) : super(context)
	constructor(
		context: Context,
		orientation: Int,
		reverseLayout: Boolean,
	) : super(context, orientation, reverseLayout)

	constructor(
		context: Context,
		attrs: AttributeSet?,
		defStyleAttr: Int,
		defStyleRes: Int,
	) : super(context, attrs, defStyleAttr, defStyleRes)

	override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State): Int {
		scrollDirection = dy.sign
		return super.scrollVerticallyBy(dy, recycler, state)
	}

	override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
		if (state.hasTargetScrollPosition()) {
			super.calculateExtraLayoutSpace(state, extraLayoutSpace)
			return
		}
		val pageSize = height
		extraLayoutSpace[0] = if (scrollDirection < 0) pageSize else 0
		extraLayoutSpace[1] = if (scrollDirection < 0) 0 else pageSize
	}
}