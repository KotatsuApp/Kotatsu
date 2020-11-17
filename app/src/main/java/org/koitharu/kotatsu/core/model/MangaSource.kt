package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.koin.core.context.GlobalContext
import org.koin.core.error.NoBeanDefFoundException
import org.koin.core.qualifier.named
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.site.*
import org.koitharu.kotatsu.local.domain.LocalMangaRepository

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
	MANGALIB("MangaLib", "ru", MangaLibRepository::class.java),
	NUDEMOON("Nude-Moon", "ru", NudeMoonRepository::class.java),
	MANGAREAD("MangaRead", "en", MangareadRepository::class.java);
	// HENTAILIB("HentaiLib", "ru", HentaiLibRepository::class.java)

	@get:Throws(NoBeanDefFoundException::class)
	@Deprecated("")
	val repository: MangaRepository
		get() = GlobalContext.get().get(named(this))
}