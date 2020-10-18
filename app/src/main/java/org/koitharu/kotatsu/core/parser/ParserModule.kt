package org.koitharu.kotatsu.core.parser

import org.koin.dsl.bind
import org.koin.dsl.module

val parserModule
	get() = module {
		single { LocalMangaRepository() } bind MangaRepository::class
	}