package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.core.db.TABLE_PREFERENCES

class Migration28To29 : Migration(28, 29) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN skip_decimal_chapters INTEGER NOT NULL DEFAULT 0")
	}
}