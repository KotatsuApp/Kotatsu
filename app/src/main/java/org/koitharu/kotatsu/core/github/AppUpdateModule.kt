package org.koitharu.kotatsu.core.github

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appUpdateModule
	get() = module {
		single { AppUpdateRepository(androidContext(), get()) }
	}
