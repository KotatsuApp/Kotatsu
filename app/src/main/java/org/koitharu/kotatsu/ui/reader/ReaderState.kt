package org.koitharu.kotatsu.ui.reader

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter

@Parcelize
data class ReaderState(
	val manga: Manga,
	val chapterId: Long,
	val page: Int,
	val scroll: Int
) : Parcelable {

	@IgnoredOnParcel
	val chapter: MangaChapter? by lazy {
		manga.chapters?.find { it.id == chapterId }
	}
}