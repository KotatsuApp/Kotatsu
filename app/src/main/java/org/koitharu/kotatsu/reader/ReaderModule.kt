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
			ReaderViewModel(params[0], params[1], get(), get(), get(), get(), get())
		}
	}