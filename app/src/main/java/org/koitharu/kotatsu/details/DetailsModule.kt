package org.koitharu.kotatsu.details

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.details.ui.DetailsViewModel

val detailsModule
	get() = module {

		viewModel { intent ->
			DetailsViewModel(intent.get(), get(), get(), get(), get(), get(), get())
		}
	}