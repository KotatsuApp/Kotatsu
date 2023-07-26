package org.koitharu.kotatsu.filter.ui

import org.koitharu.kotatsu.filter.ui.model.FilterItem
import org.koitharu.kotatsu.list.ui.adapter.ListHeaderClickListener

interface OnFilterChangedListener : ListHeaderClickListener {

	fun onSortItemClick(item: FilterItem.Sort)

	fun onTagItemClick(item: FilterItem.Tag)
}
