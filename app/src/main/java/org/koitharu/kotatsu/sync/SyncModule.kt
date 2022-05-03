package org.koitharu.kotatsu.sync

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.sync.data.SyncAuthApi
import org.koitharu.kotatsu.sync.ui.SyncAuthViewModel

val syncModule
	get() = module {

		factory { SyncAuthApi(androidContext(), get()) }

		viewModel { SyncAuthViewModel(get()) }
	}