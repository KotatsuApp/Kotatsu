package org.koitharu.kotatsu.reader

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.reader.ui.ReaderViewModel

val readerModule
	get() = module {

		single { MangaDataRepository(get()) }
		single { PagesCache(get()) }

		viewModel { params ->
			ReaderViewModel(
				intent = params[0],
				initialState = params[1],
				preselectedBranch = params[2],
				dataRepository = get(),
				historyRepository = get(),
				shortcutsRepository = get(),
				settings = get(),
				downloadManagerHelper = get(),
			)
		}
	}