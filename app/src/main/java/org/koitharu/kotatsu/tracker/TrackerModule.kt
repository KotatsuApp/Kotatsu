package org.koitharu.kotatsu.tracker

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.ui.FeedViewModel

val trackerModule
	get() = module {

		single { TrackingRepository(get(), get()) }

		viewModel { FeedViewModel(androidContext(), get()) }
	}