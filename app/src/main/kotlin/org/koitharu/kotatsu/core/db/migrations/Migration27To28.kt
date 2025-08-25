package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration27To28 : Migration(27, 28) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL(
			"""CREATE TABLE IF NOT EXISTS translation_preferences (
				manga_id INTEGER NOT NULL,
				branch TEXT NOT NULL,
				priority INTEGER NOT NULL,
				is_enabled INTEGER NOT NULL,
				last_used INTEGER,
				PRIMARY KEY(manga_id, branch),
				FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON DELETE CASCADE
			)""",
		)
		db.execSQL("CREATE INDEX IF NOT EXISTS index_translation_preferences_manga_id ON translation_preferences(manga_id)")
	}
}