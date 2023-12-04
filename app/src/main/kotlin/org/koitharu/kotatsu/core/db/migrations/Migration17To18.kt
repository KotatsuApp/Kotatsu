package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration17To18 : Migration(17, 18) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE preferences ADD COLUMN `cf_grayscale` INTEGER NOT NULL DEFAULT 0")
	}
}
