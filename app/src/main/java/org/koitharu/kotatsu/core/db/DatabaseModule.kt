package org.koitharu.kotatsu.core.db

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koitharu.kotatsu.core.db.migrations.*

val databaseModule
	get() = module {
		single {
			Room.databaseBuilder(
				androidContext(),
				MangaDatabase::class.java,
				"kotatsu-db"
			).addMigrations(
				Migration1To2(),
				Migration2To3(),
				Migration3To4(),
				Migration4To5(),
				Migration5To6(),
				Migration6To7(),
				Migration7To8(),
				Migration8To9(),
			).addCallback(
				DatabasePrePopulateCallback(androidContext().resources)
			).build()
		}
	}