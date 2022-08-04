package org.koitharu.kotatsu.tracker

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.tracker.domain.Tracker
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.ui.FeedViewModel
import org.koitharu.kotatsu.tracker.work.TrackerNotificationChannels

val trackerModule
	get() = module {

		factory { TrackingRepository(get()) }
		factory { TrackerNotificationChannels(androidContext(), get()) }

		factory { Tracker(get(), get(), get(), get()) }

		viewModel { FeedViewModel(get()) }
	}