package org.koitharu.kotatsu.core.ui.list

import android.view.View

fun interface OnListItemClickListener<I> {

	fun onItemClick(item: I, view: View)

	fun onItemLongClick(item: I, view: View) = false
}
