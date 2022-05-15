package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration11To12 : Migration(11, 12) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE favourite_categories ADD COLUMN `deleted_at` INTEGER NOT NULL DEFAULT 0")
		database.execSQL("ALTER TABLE favourites ADD COLUMN `deleted_at` INTEGER NOT NULL DEFAULT 0")
		database.execSQL("ALTER TABLE history ADD COLUMN `deleted_at` INTEGER NOT NULL DEFAULT 0")
	}
}