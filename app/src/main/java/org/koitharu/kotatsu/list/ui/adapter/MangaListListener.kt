package org.koitharu.kotatsu.list.ui.adapter

import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaTag

interface MangaListListener : OnListItemClickListener<Manga> {

	fun onRetryClick(error: Throwable)
	fun onTagRemoveClick(tag: MangaTag)
	fun onFilterClick()
	fun onEmptyActionClick()
}