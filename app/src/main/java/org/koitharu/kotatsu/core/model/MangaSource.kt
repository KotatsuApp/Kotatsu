package org.koitharu.kotatsu.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
	val cls: Class<out MangaRepository>,
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

	// NUDEMOON("Nude-Moon", "ru", NudeMoonRepository::class.java),
	MANGAREAD("MangaRead", "en", MangareadRepository::class.java),
	REMANGA("Remanga", "ru", RemangaRepository::class.java),
	HENTAILIB("HentaiLib", "ru", HentaiLibRepository::class.java),
	ANIBEL("Anibel", "be", AnibelRepository::class.java),
	NINEMANGA_EN("NineManga English", "en", NineMangaRepository.English::class.java),
	NINEMANGA_ES("NineManga Español", "es", NineMangaRepository.Spanish::class.java),
	NINEMANGA_RU("NineManga Русский", "ru", NineMangaRepository.Russian::class.java),
	NINEMANGA_DE("NineManga Deutsch", "de", NineMangaRepository.Deutsch::class.java),
	NINEMANGA_IT("NineManga Italiano", "it", NineMangaRepository.Italiano::class.java),
	NINEMANGA_BR("NineManga Brasil", "pt", NineMangaRepository.Brazil::class.java),
	NINEMANGA_FR("NineManga Français", "fr", NineMangaRepository.Francais::class.java),
	EXHENTAI("ExHentai", null, ExHentaiRepository::class.java),
	MANGAOWL("MangaOwl", "en", MangaOwlRepository::class.java),
	MANGADEX("MangaDex", null, MangaDexRepository::class.java),
	;

	@get:Throws(NoBeanDefFoundException::class)
	@Deprecated("", ReplaceWith("MangaRepository(this)",
		"org.koitharu.kotatsu.core.parser.MangaRepository"))
	val repository: MangaRepository
		get() = GlobalContext.get().get(named(this))
}