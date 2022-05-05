package org.koitharu.kotatsu.reader

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.reader.ui.PageSaveHelper
import org.koitharu.kotatsu.reader.ui.ReaderViewModel

val readerModule
	get() = module {

		factory { MangaDataRepository(get()) }
		single { PagesCache(get()) }

		factory { PageSaveHelper(get(), androidContext()) }

		viewModel { params ->
			ReaderViewModel(
				intent = params[0],
				initialState = params[1],
				preselectedBranch = params[2],
				dataRepository = get(),
				historyRepository = get(),
				shortcutsRepository = get(),
				settings = get(),
				pageSaveHelper = get(),
			)
		}
	}