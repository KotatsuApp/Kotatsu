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

		single { MangaSearchRepository() }

		viewModel { (source: MangaSource) -> SearchViewModel(get(named(source))) }
		viewModel { GlobalSearchViewModel(get()) }
	}