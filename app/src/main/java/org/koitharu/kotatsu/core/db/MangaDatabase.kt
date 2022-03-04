package org.koitharu.kotatsu.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.koitharu.kotatsu.core.db.dao.*
import org.koitharu.kotatsu.core.db.entity.*
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
	], version = 9
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