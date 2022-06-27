package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration11To12 : Migration(11, 12) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE history ADD COLUMN `percent` REAL NOT NULL DEFAULT -1")
		database.execSQL("ALTER TABLE bookmarks ADD COLUMN `percent` REAL NOT NULL DEFAULT -1")
	}
}