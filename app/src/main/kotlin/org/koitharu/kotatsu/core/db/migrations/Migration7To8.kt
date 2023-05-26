package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration7To8 : Migration(7, 8) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE manga ADD COLUMN nsfw INTEGER NOT NULL DEFAULT 0")
		database.execSQL("CREATE TABLE IF NOT EXISTS suggestions (manga_id INTEGER NOT NULL, relevance REAL NOT NULL, created_at INTEGER NOT NULL, PRIMARY KEY(manga_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE )")
		database.execSQL("CREATE INDEX IF NOT EXISTS index_suggestions_manga_id ON suggestions (manga_id)")
	}
}