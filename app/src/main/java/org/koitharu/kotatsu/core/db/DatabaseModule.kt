package org.koitharu.kotatsu.core.db

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule
	get() = module {
		single { MangaDatabase(androidContext()) }
	}
