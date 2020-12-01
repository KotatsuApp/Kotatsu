package org.koitharu.kotatsu.remotelist

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel

val remoteListModule
	get() = module {

		viewModel { (source: MangaSource) ->
			RemoteListViewModel(get(named(source)), get())
		}
	}