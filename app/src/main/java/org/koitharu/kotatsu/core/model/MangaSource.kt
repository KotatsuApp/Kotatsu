package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.site.MintMangaRepository
import org.koitharu.kotatsu.core.parser.site.ReadmangaRepository

@Parcelize
enum class MangaSource(val title: String, val cls: Class<out MangaRepository>): Parcelable {
	READMANGA_RU("ReadManga", ReadmangaRepository::class.java),
	MINTMANGA("MintManga", MintMangaRepository::class.java)
}