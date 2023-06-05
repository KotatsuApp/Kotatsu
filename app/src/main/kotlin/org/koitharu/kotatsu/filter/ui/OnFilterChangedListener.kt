package org.koitharu.kotatsu.filter.ui

import org.koitharu.kotatsu.filter.ui.model.FilterItem

interface OnFilterChangedListener {

	fun onSortItemClick(item: FilterItem.Sort)

	fun onTagItemClick(item: FilterItem.Tag)
}
