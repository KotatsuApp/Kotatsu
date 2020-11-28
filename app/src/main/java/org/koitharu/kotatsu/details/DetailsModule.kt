package org.koitharu.kotatsu.details

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.details.ui.DetailsViewModel

val detailsModule
	get() = module {

		viewModel { (intent: MangaIntent) ->
			DetailsViewModel(intent, get(), get(), get(), get(), get())
		}
	}