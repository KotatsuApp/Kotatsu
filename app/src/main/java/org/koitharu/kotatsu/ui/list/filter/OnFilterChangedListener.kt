package org.koitharu.kotatsu.ui.list.filter

import org.koitharu.kotatsu.core.model.MangaFilter

fun interface OnFilterChangedListener {

	fun onFilterChanged(filter: MangaFilter)
}