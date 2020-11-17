package org.koitharu.kotatsu.favourites

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.FavouritesCategoriesViewModel
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListViewModel

val favouritesModule
	get() = module {

		single { FavouritesRepository(get()) }

		viewModel { (categoryId: Long) ->
			FavouritesListViewModel(categoryId, get())
		}
		viewModel { FavouritesCategoriesViewModel(get()) }
	}