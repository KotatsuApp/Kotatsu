package org.koitharu.kotatsu.ui.common.list

import android.view.View

interface OnRecyclerItemClickListener<I> {

	fun onItemClick(item: I, position: Int, view: View)

	fun onItemLongClick(item: I, position: Int, view: View) = false
}