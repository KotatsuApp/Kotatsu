package org.koitharu.kotatsu.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.koitharu.kotatsu.core.db.entity.HistoryEntity

@Database(entities = [HistoryEntity::class], version = 1)
abstract class MangaDatabase : RoomDatabase()