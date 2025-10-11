package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.core.db.TABLE_PREFERENCES

class Migration29To30 : Migration(29, 30) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN last_applied_translation_languages TEXT")
	}
}