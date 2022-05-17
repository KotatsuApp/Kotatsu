package org.koitharu.kotatsu.main

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.base.ui.util.ActivityRecreationHandle
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.main.ui.MainViewModel
import org.koitharu.kotatsu.main.ui.protect.AppProtectHelper
import org.koitharu.kotatsu.main.ui.protect.ProtectViewModel

val mainModule
	get() = module {
		single { AppProtectHelper(get()) }
		single { ActivityRecreationHandle() }
		factory { ShortcutsRepository(androidContext(), get(), get(), get()) }
		viewModel { MainViewModel(get(), get()) }
		viewModel { ProtectViewModel(get(), get()) }
	}