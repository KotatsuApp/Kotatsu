package org.koitharu.kotatsu.suggestions

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.suggestions.ui.SuggestionsViewModel

val suggestionsModule
	get() = module {

		factory { SuggestionRepository(get()) }

		viewModel { SuggestionsViewModel(get(), get()) }
	}