package org.koitharu.kotatsu.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.koitharu.kotatsu.core.db.entity.HistoryEntity
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Database(entities = [MangaEntity::class, TagEntity::class, HistoryEntity::class], version = 1)
abstract class MangaDatabase : RoomDatabase() {

	abstract fun historyDao(): HistoryDao

	abstract fun tagsDao(): TagsDao
}