package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration4To5 : Migration(4, 5) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE favourite_categories ADD COLUMN sort_key INTEGER NOT NULL DEFAULT 0")
	}
}