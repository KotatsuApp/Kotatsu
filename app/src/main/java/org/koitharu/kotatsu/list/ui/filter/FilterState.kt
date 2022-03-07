package org.koitharu.kotatsu.list.ui.filter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder

@Parcelize
class FilterState(
	val sortOrder: SortOrder?,
	val tags: Set<MangaTag>,
) : Parcelable