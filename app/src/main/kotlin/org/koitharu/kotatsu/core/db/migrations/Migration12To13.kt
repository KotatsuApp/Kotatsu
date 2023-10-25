package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration12To13 : Migration(12, 13) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE favourite_categories ADD COLUMN `show_in_lib` INTEGER NOT NULL DEFAULT 1")
		db.execSQL("ALTER TABLE favourites ADD COLUMN `sort_key` INTEGER NOT NULL DEFAULT 0")
	}
}
