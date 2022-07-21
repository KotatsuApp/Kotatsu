package org.koitharu.kotatsu.main

import android.app.Application
import android.os.Build
import androidx.room.InvalidationTracker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.base.ui.util.ActivityRecreationHandle
import org.koitharu.kotatsu.core.os.ShortcutsUpdater
import org.koitharu.kotatsu.main.ui.MainViewModel
import org.koitharu.kotatsu.main.ui.protect.AppProtectHelper
import org.koitharu.kotatsu.main.ui.protect.ProtectViewModel

val mainModule
	get() = module {
		single { AppProtectHelper(get()) } bind Application.ActivityLifecycleCallbacks::class
		single { ActivityRecreationHandle() } bind Application.ActivityLifecycleCallbacks::class

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			single { ShortcutsUpdater(androidContext(), get(), get(), get()) } bind InvalidationTracker.Observer::class
		} else {
			factory { ShortcutsUpdater(androidContext(), get(), get(), get()) }
		}

		viewModel { MainViewModel(get(), get(), get(), get()) }
		viewModel { ProtectViewModel(get(), get()) }
	}
