package org.koitharu.kotatsu.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.bookmarks.data.BookmarkEntity
import org.koitharu.kotatsu.bookmarks.data.BookmarksDao
import org.koitharu.kotatsu.core.db.dao.MangaDao
import org.koitharu.kotatsu.core.db.dao.MangaSourcesDao
import org.koitharu.kotatsu.core.db.dao.PreferencesDao
import org.koitharu.kotatsu.core.db.dao.TagsDao
import org.koitharu.kotatsu.core.db.dao.TrackLogsDao
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.db.entity.MangaSourceEntity
import org.koitharu.kotatsu.core.db.entity.MangaTagsEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.db.migrations.Migration10To11
import org.koitharu.kotatsu.core.db.migrations.Migration11To12
import org.koitharu.kotatsu.core.db.migrations.Migration12To13
import org.koitharu.kotatsu.core.db.migrations.Migration13To14
import org.koitharu.kotatsu.core.db.migrations.Migration14To15
import org.koitharu.kotatsu.core.db.migrations.Migration15To16
import org.koitharu.kotatsu.core.db.migrations.Migration16To17
import org.koitharu.kotatsu.core.db.migrations.Migration17To18
import org.koitharu.kotatsu.core.db.migrations.Migration18To19
import org.koitharu.kotatsu.core.db.migrations.Migration19To20
import org.koitharu.kotatsu.core.db.migrations.Migration1To2
import org.koitharu.kotatsu.core.db.migrations.Migration20To21
import org.koitharu.kotatsu.core.db.migrations.Migration21To22
import org.koitharu.kotatsu.core.db.migrations.Migration2To3
import org.koitharu.kotatsu.core.db.migrations.Migration3To4
import org.koitharu.kotatsu.core.db.migrations.Migration4To5
import org.koitharu.kotatsu.core.db.migrations.Migration5To6
import org.koitharu.kotatsu.core.db.migrations.Migration6To7
import org.koitharu.kotatsu.core.db.migrations.Migration7To8
import org.koitharu.kotatsu.core.db.migrations.Migration8To9
import org.koitharu.kotatsu.core.db.migrations.Migration9To10
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.favourites.data.FavouriteCategoriesDao
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.favourites.data.FavouritesDao
import org.koitharu.kotatsu.history.data.HistoryDao
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblingDao
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblingEntity
import org.koitharu.kotatsu.stats.data.StatsDao
import org.koitharu.kotatsu.stats.data.StatsEntity
import org.koitharu.kotatsu.suggestions.data.SuggestionDao
import org.koitharu.kotatsu.suggestions.data.SuggestionEntity
import org.koitharu.kotatsu.tracker.data.TrackEntity
import org.koitharu.kotatsu.tracker.data.TrackLogEntity
import org.koitharu.kotatsu.tracker.data.TracksDao

const val DATABASE_VERSION = 22

@Database(
	entities = [
		MangaEntity::class, TagEntity::class, HistoryEntity::class, MangaTagsEntity::class,
		FavouriteCategoryEntity::class, FavouriteEntity::class, MangaPrefsEntity::class,
		TrackEntity::class, TrackLogEntity::class, SuggestionEntity::class, BookmarkEntity::class,
		ScrobblingEntity::class, MangaSourceEntity::class, StatsEntity::class,
	],
	version = DATABASE_VERSION,
)
abstract class MangaDatabase : RoomDatabase() {

	abstract fun getHistoryDao(): HistoryDao

	abstract fun getTagsDao(): TagsDao

	abstract fun getMangaDao(): MangaDao

	abstract fun getFavouritesDao(): FavouritesDao

	abstract fun getPreferencesDao(): PreferencesDao

	abstract fun getFavouriteCategoriesDao(): FavouriteCategoriesDao

	abstract fun getTracksDao(): TracksDao

	abstract fun getTrackLogsDao(): TrackLogsDao

	abstract fun getSuggestionDao(): SuggestionDao

	abstract fun getBookmarksDao(): BookmarksDao

	abstract fun getScrobblingDao(): ScrobblingDao

	abstract fun getSourcesDao(): MangaSourcesDao

	abstract fun getStatsDao(): StatsDao
}

fun getDatabaseMigrations(context: Context): Array<Migration> = arrayOf(
	Migration1To2(),
	Migration2To3(),
	Migration3To4(),
	Migration4To5(),
	Migration5To6(),
	Migration6To7(),
	Migration7To8(),
	Migration8To9(),
	Migration9To10(),
	Migration10To11(),
	Migration11To12(),
	Migration12To13(),
	Migration13To14(),
	Migration14To15(),
	Migration15To16(),
	Migration16To17(context),
	Migration17To18(),
	Migration18To19(),
	Migration19To20(),
	Migration20To21(),
	Migration21To22(),
)

fun MangaDatabase(context: Context): MangaDatabase = Room
	.databaseBuilder(context, MangaDatabase::class.java, "kotatsu-db")
	.addMigrations(*getDatabaseMigrations(context))
	.addCallback(DatabasePrePopulateCallback(context.resources))
	.build()

fun InvalidationTracker.removeObserverAsync(observer: InvalidationTracker.Observer) {
	val scope = processLifecycleScope
	if (scope.isActive) {
		processLifecycleScope.launch(Dispatchers.Default, CoroutineStart.ATOMIC) {
			removeObserver(observer)
		}
	}
}
