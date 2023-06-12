package org.koitharu.kotatsu.list.ui.adapter

import android.view.View
import org.koitharu.kotatsu.parsers.model.MangaTag

interface MangaListListener : MangaDetailsClickListener, ListStateHolderListener, ListHeaderClickListener {

	fun onUpdateFilter(tags: Set<MangaTag>)

	fun onFilterClick(view: View?)
}
