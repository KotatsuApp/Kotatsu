package org.koitharu.kotatsu.main

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.main.ui.MainViewModel
import org.koitharu.kotatsu.main.ui.protect.ProtectViewModel

val mainModule
	get() = module {
		viewModel { MainViewModel(get()) }
		viewModel { ProtectViewModel(get()) }
	}