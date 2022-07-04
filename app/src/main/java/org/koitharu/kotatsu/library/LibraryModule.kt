package org.koitharu.kotatsu.library

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.library.ui.LibraryViewModel

val libraryModule
	get() = module {

		viewModel { LibraryViewModel(get(), get(), get(), get(), get()) }
	}