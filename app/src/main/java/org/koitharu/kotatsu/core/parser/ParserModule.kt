package org.koitharu.kotatsu.core.parser

import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.site.*

val parserModule
	get() = module {

		single { MangaLoaderContext(get(), get()) }

		factory<MangaRepository>(named(MangaSource.READMANGA_RU)) { ReadmangaRepository(get()) }
		factory<MangaRepository>(named(MangaSource.MINTMANGA)) { MintMangaRepository(get()) }
		factory<MangaRepository>(named(MangaSource.SELFMANGA)) { SelfMangaRepository(get()) }
		factory<MangaRepository>(named(MangaSource.MANGACHAN)) { MangaChanRepository(get()) }
		factory<MangaRepository>(named(MangaSource.DESUME)) { DesuMeRepository(get()) }
		factory<MangaRepository>(named(MangaSource.HENCHAN)) { HenChanRepository(get()) }
		factory<MangaRepository>(named(MangaSource.YAOICHAN)) { YaoiChanRepository(get()) }
		factory<MangaRepository>(named(MangaSource.MANGATOWN)) { MangaTownRepository(get()) }
		factory<MangaRepository>(named(MangaSource.MANGALIB)) { MangaLibRepository(get()) }
		// factory<MangaRepository>(named(MangaSource.NUDEMOON)) { NudeMoonRepository(get()) }
		factory<MangaRepository>(named(MangaSource.MANGAREAD)) { MangareadRepository(get()) }
		factory<MangaRepository>(named(MangaSource.REMANGA)) { RemangaRepository(get()) }
		factory<MangaRepository>(named(MangaSource.HENTAILIB)) { HentaiLibRepository(get()) }
	}