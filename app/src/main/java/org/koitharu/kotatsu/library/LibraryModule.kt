package org.koitharu.kotatsu.library

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.library.domain.LibraryRepository
import org.koitharu.kotatsu.library.ui.LibraryViewModel
import org.koitharu.kotatsu.library.ui.config.LibraryCategoriesConfigViewModel

val libraryModule
	get() = module {

		factory { LibraryRepository(get()) }

		viewModel { LibraryViewModel(get(), get(), get(), get()) }
		viewModel { LibraryCategoriesConfigViewModel(get()) }
	}