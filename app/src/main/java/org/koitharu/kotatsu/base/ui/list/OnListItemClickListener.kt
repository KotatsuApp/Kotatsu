package org.koitharu.kotatsu.base.ui.list

import android.view.View

fun interface OnListItemClickListener<I> {

	fun onItemClick(item: I, view: View)

	fun onItemLongClick(item: I, view: View) = false
}
