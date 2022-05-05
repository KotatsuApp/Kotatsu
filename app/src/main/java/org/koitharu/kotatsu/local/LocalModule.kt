package org.koitharu.kotatsu.local

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.download.domain.DownloadManager
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.local.ui.LocalListViewModel

val localModule
	get() = module {

		factory { LocalStorageManager(androidContext(), get()) }
		single { LocalMangaRepository(get()) }

		factory { DownloadManager.Factory(androidContext(), get(), get(), get(), get(), get()) }

		viewModel { LocalListViewModel(get(), get(), get(), get()) }
	}