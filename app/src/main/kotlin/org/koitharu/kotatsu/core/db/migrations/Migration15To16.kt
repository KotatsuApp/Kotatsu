package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration15To16 : Migration(15, 16) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE preferences ADD COLUMN `cf_invert` INTEGER NOT NULL DEFAULT 0")
	}
}
