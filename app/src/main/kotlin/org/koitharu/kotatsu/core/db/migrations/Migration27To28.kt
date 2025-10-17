package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.core.db.TABLE_PREFERENCES

class Migration27To28 : Migration(27, 28) {
	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE `$TABLE_PREFERENCES` ADD COLUMN `incognito_mode` INTEGER NOT NULL DEFAULT 0")
	}
}