package org.koitharu.kotatsu.explore

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.explore.ui.ExploreViewModel

val exploreModule
	get() = module {
		viewModel { ExploreViewModel(get()) }
	}