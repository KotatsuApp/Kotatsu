package org.koitharu.kotatsu.list.ui.filter

import org.koitharu.kotatsu.core.model.MangaFilter

fun interface OnFilterChangedListener {

	fun onFilterChanged(filter: MangaFilter)
}