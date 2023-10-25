package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.parsers.model.SortOrder

class Migration8To9 : Migration(8, 9) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE favourite_categories ADD COLUMN `order` TEXT NOT NULL DEFAULT ${SortOrder.NEWEST.name}")
	}
}
