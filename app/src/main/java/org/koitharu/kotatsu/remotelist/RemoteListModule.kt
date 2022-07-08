package org.koitharu.kotatsu.remotelist

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel

val remoteListModule
	get() = module {

		viewModel { params ->
			RemoteListViewModel(
				repository = MangaRepository(params[0]) as RemoteMangaRepository,
				settings = get(),
				dataRepository = get(),
				searchRepository = get(),
			)
		}
	}