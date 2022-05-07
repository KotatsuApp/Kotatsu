package org.koitharu.kotatsu.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koitharu.kotatsu.core.db.dao.*
import org.koitharu.kotatsu.core.db.entity.*
import org.koitharu.kotatsu.core.db.migrations.*
import org.koitharu.kotatsu.favourites.data.FavouriteCategoriesDao
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.favourites.data.FavouritesDao
import org.koitharu.kotatsu.history.data.HistoryDao
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.suggestions.data.SuggestionDao
import org.koitharu.kotatsu.suggestions.data.SuggestionEntity

@Database(
	entities = [
		MangaEntity::class, TagEntity::class, HistoryEntity::class, MangaTagsEntity::class,
		FavouriteCategoryEntity::class, FavouriteEntity::class, MangaPrefsEntity::class,
		TrackEntity::class, TrackLogEntity::class, SuggestionEntity::class
	],
	version = 10
)
abstract class MangaDatabase : RoomDatabase() {

	abstract val historyDao: HistoryDao

	abstract val tagsDao: TagsDao

	abstract val mangaDao: MangaDao

	abstract val favouritesDao: FavouritesDao

	abstract val preferencesDao: PreferencesDao

	abstract val favouriteCategoriesDao: FavouriteCategoriesDao

	abstract val tracksDao: TracksDao

	abstract val trackLogsDao: TrackLogsDao

	abstract val suggestionDao: SuggestionDao
}

fun MangaDatabase(context: Context): MangaDatabase = Room.databaseBuilder(
	context,
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
	Migration9To10(),
).addCallback(
	DatabasePrePopulateCallback(context.resources)
).build()