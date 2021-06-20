package org.koitharu.kotatsu.search

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.MangaSuggestionsProvider
import org.koitharu.kotatsu.search.ui.SearchViewModel
import org.koitharu.kotatsu.search.ui.global.GlobalSearchViewModel
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionViewModel

val searchModule
	get() = module {

		single { MangaSearchRepository(get(), get(), androidContext(), get()) }

		factory { MangaSuggestionsProvider.createSuggestions(androidContext()) }

		viewModel { params ->
			SearchViewModel(get(named(params[0])), params[1], get())
		}
		viewModel { query ->
			GlobalSearchViewModel(query.get(), get(), get())
		}
		viewModel { SearchSuggestionViewModel(get()) }
	}