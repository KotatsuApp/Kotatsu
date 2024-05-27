package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration20To21 : Migration(20, 21) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE tracks ADD COLUMN `last_error` TEXT DEFAULT NULL")
		db.execSQL("ALTER TABLE sources ADD COLUMN `added_in` INTEGER NOT NULL DEFAULT 0")
	}
}
