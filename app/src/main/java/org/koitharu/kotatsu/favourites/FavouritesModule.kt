package org.koitharu.kotatsu.favourites

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.FavouritesCategoriesViewModel
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditViewModel
import org.koitharu.kotatsu.favourites.ui.categories.select.MangaCategoriesViewModel
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListViewModel

val favouritesModule
	get() = module {

		single { FavouritesRepository(get(), get()) }

		viewModel { categoryId ->
			FavouritesListViewModel(categoryId.get(), get(), get(), get(), get())
		}
		viewModel { FavouritesCategoriesViewModel(get(), get()) }
		viewModel { manga ->
			MangaCategoriesViewModel(manga.get(), get())
		}
		viewModel { params -> FavouritesCategoryEditViewModel(params[0], get(), get()) }
	}