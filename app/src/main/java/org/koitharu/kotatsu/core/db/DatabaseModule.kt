package org.koitharu.kotatsu.core.db

import androidx.room.InvalidationTracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule
	get() = module {
		single {
			MangaDatabase(androidContext()).also { db ->
				getAll<InvalidationTracker.Observer>().forEach {
					db.invalidationTracker.addObserver(it)
				}
			}
		}
	}