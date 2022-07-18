package org.koitharu.kotatsu.explore

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.explore.domain.ExploreRepository
import org.koitharu.kotatsu.explore.ui.ExploreViewModel

val exploreModule
	get() = module {

		factory { ExploreRepository(get(), get()) }

		viewModel { ExploreViewModel(get(), get()) }
	}