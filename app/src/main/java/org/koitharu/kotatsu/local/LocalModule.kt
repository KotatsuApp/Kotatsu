package org.koitharu.kotatsu.local

import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.local.ui.LocalListViewModel

val localModule
	get() = module {

		single { LocalMangaRepository(androidContext()) } bind MangaRepository::class

		viewModel { LocalListViewModel(get(), get(), get(), androidContext()) }
	}