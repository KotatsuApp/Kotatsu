package org.koitharu.kotatsu.sync

import androidx.room.InvalidationTracker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.sync.data.SyncAuthApi
import org.koitharu.kotatsu.sync.domain.SyncController
import org.koitharu.kotatsu.sync.ui.SyncAuthViewModel

val syncModule
	get() = module {

		single { SyncController(androidContext()) } bind InvalidationTracker.Observer::class

		factory { SyncAuthApi(androidContext(), get()) }

		viewModel { SyncAuthViewModel(get()) }
	}