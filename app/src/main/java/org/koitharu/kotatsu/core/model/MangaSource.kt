package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.site.*

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
	SELFMANGA("SelfManga", "ru", SelfMangaRepository::class.java),
	MANGACHAN("Манга-тян", "ru", MangaChanRepository::class.java),
	DESUME("Desu.me", "ru", DesuMeRepository::class.java),
	HENCHAN("Хентай-тян", "ru", HenChanRepository::class.java),
	YAOICHAN("Яой-тян", "ru", YaoiChanRepository::class.java),
	MANGATOWN("MangaTown", "en", MangaTownRepository::class.java),
	MANGALIB("MangaLib", "ru", MangaLibRepository::class.java)
	// HENTAILIB("HentaiLib", "ru", HentaiLibRepository::class.java)
}