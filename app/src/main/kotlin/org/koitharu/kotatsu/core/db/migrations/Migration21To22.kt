package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration21To22 : Migration(21, 22) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE sources ADD COLUMN `used_at` INTEGER NOT NULL DEFAULT 0")
		db.execSQL("ALTER TABLE sources ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
	}
}
