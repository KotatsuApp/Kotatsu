package org.koitharu.kotatsu.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.koitharu.kotatsu.core.db.entity.*

@Database(
	entities = [
		MangaEntity::class, TagEntity::class, HistoryEntity::class, MangaTagsEntity::class,
		FavouriteCategoryEntity::class, FavouriteEntity::class, MangaPrefsEntity::class
	], version = 2
)
abstract class MangaDatabase : RoomDatabase() {

	abstract fun historyDao(): HistoryDao

	abstract fun tagsDao(): TagsDao

	abstract fun mangaDao(): MangaDao

	abstract fun favouritesDao(): FavouritesDao

	abstract fun preferencesDao(): PreferencesDao

	abstract fun favouriteCategoriesDao(): FavouriteCategoriesDao
}