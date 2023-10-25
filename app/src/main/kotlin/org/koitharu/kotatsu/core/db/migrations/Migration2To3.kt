package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration2To3 : Migration(2, 3) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE history ADD COLUMN scroll REAL NOT NULL DEFAULT 0")
	}
}
