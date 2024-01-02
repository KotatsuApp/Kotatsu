package org.koitharu.kotatsu.filter.ui

import org.koitharu.kotatsu.list.ui.adapter.ListHeaderClickListener
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.Locale

interface OnFilterChangedListener : ListHeaderClickListener {

	fun setSortOrder(value: SortOrder)

	fun setLanguage(value: Locale?)

	fun setTag(value: MangaTag, addOrRemove: Boolean)

	fun setTagExcluded(value: MangaTag, addOrRemove: Boolean)

	fun setState(value: MangaState, addOrRemove: Boolean)

	fun setContentRating(value: ContentRating, addOrRemove: Boolean)
}
