package org.koitharu.kotatsu.local

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.local.ui.LocalListViewModel

val localModule
	get() = module {

		single { LocalMangaRepository(androidContext()) }
		factory<MangaRepository>(named(MangaSource.LOCAL)) { get<LocalMangaRepository>() }

		viewModel { LocalListViewModel(get(), get(), get(), get(), androidContext()) }
	}