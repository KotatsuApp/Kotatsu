package org.koitharu.kotatsu.list.ui.filter

interface OnFilterChangedListener {

	fun onSortItemClick(item: FilterItem.Sort)

	fun onTagItemClick(item: FilterItem.Tag)
}