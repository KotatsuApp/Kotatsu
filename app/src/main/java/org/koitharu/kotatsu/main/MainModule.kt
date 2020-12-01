package org.koitharu.kotatsu.main

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.main.ui.MainViewModel
import org.koitharu.kotatsu.main.ui.protect.ProtectViewModel

val mainModule
	get() = module {
		viewModel { MainViewModel(get(), get()) }
		viewModel { ProtectViewModel(get()) }
	}