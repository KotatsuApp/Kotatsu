package org.koitharu.kotatsu.widget

import androidx.room.InvalidationTracker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.widget.shelf.ShelfConfigViewModel

val appWidgetModule
	get() = module {

		single<InvalidationTracker.Observer> { WidgetUpdater(androidContext()) }
	
		viewModel { ShelfConfigViewModel(get()) }
	}