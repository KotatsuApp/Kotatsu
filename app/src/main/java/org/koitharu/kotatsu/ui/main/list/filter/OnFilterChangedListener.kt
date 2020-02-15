package org.koitharu.kotatsu.ui.main.list.filter

import org.koitharu.kotatsu.core.model.MangaFilter

interface OnFilterChangedListener {

	fun onFilterChanged(filter: MangaFilter)
}