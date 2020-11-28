package org.koitharu.kotatsu.search

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.SearchViewModel
import org.koitharu.kotatsu.search.ui.global.GlobalSearchViewModel

val searchModule
	get() = module {

		single { MangaSearchRepository(get()) }

		viewModel { (source: MangaSource, query: String) ->
			SearchViewModel(get(named(source)), query, get())
		}
		viewModel { (query: String) ->
			GlobalSearchViewModel(query, get(), get())
		}
	}