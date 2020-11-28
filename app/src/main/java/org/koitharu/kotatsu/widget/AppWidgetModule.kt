package org.koitharu.kotatsu.widget

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.widget.shelf.ShelfConfigViewModel

val appWidgetModule
	get() = module {
		viewModel { ShelfConfigViewModel(get()) }
	}