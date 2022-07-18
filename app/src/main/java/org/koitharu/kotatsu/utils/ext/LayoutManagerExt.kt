package org.koitharu.kotatsu.utils.ext

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

internal val RecyclerView.LayoutManager?.firstVisibleItemPosition
	get() = when (this) {
		is LinearLayoutManager -> findFirstVisibleItemPosition()
		is StaggeredGridLayoutManager -> findFirstVisibleItemPositions(null)[0]
		else -> 0
	}

internal val RecyclerView.LayoutManager?.isLayoutReversed
	get() = when (this) {
		is LinearLayoutManager -> reverseLayout
		is StaggeredGridLayoutManager -> reverseLayout
		else -> false
	}