package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.site.MintMangaRepository
import org.koitharu.kotatsu.core.parser.site.ReadmangaRepository
import org.koitharu.kotatsu.core.parser.site.SelfMangaRepository

@Suppress("SpellCheckingInspection")
@Parcelize
enum class MangaSource(
	val title: String,
	val locale: String?,
	val cls: Class<out MangaRepository>
) : Parcelable {
	LOCAL("Local", null, LocalMangaRepository::class.java),
	READMANGA_RU("ReadManga", "ru", ReadmangaRepository::class.java),
	MINTMANGA("MintManga", "ru", MintMangaRepository::class.java),
	SELFMANGA("SelfManga", "ru", SelfMangaRepository::class.java)
}