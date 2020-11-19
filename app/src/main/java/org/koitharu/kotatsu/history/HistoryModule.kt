package org.koitharu.kotatsu.history

import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.ui.HistoryListViewModel

val historyModule
	get() = module {

		single { HistoryRepository(get()) }
		viewModel { HistoryListViewModel(get(), androidContext(), get()) }
	}