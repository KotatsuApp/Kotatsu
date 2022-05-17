package org.koitharu.kotatsu.search

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.MangaSuggestionsProvider
import org.koitharu.kotatsu.search.ui.SearchViewModel
import org.koitharu.kotatsu.search.ui.multi.MultiSearchViewModel
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionViewModel

val searchModule
	get() = module {

		factory { MangaSearchRepository(get(), get(), androidContext(), get()) }
		factory { MangaSuggestionsProvider.createSuggestions(androidContext()) }

		viewModel { params -> SearchViewModel(MangaRepository(params[0]), params[1], get()) }
		viewModel { SearchSuggestionViewModel(get(), get()) }
		viewModel { params -> MultiSearchViewModel(params[0], get()) }
	}