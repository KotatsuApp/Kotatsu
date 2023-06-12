package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration14To15 : Migration(14, 15) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE preferences ADD COLUMN `cf_brightness` REAL NOT NULL DEFAULT 0")
		database.execSQL("ALTER TABLE preferences ADD COLUMN `cf_contrast` REAL NOT NULL DEFAULT 0")
	}
}
