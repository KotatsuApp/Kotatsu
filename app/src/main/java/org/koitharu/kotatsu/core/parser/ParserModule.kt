package org.koitharu.kotatsu.core.parser

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.site.*

val parserModule
	get() = module {
		single { LocalMangaRepository(get()) } bind MangaRepository::class

		factory { ReadmangaRepository(get()) } bind MangaRepository::class
		factory { MintMangaRepository(get()) } bind MangaRepository::class
		factory { SelfMangaRepository(get()) } bind MangaRepository::class
		factory { MangaChanRepository(get()) } bind MangaRepository::class
		factory { DesuMeRepository(get()) } bind MangaRepository::class
		factory { HenChanRepository(get()) } bind MangaRepository::class
		factory { YaoiChanRepository(get()) } bind MangaRepository::class
		factory { MangaTownRepository(get()) } bind MangaRepository::class
		factory { MangaLibRepository(get()) } bind MangaRepository::class
		factory { NudeMoonRepository(get()) } bind MangaRepository::class
		factory { MangareadRepository(get()) } bind MangaRepository::class
	}