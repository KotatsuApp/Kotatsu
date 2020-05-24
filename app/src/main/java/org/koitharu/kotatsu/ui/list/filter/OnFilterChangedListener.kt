package org.koitharu.kotatsu.ui.list.filter

import org.koitharu.kotatsu.core.model.MangaFilter

interface OnFilterChangedListener {

	fun onFilterChanged(filter: MangaFilter)
}