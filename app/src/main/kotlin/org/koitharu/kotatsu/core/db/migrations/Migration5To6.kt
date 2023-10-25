package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration5To6 : Migration(5, 6) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("CREATE TABLE IF NOT EXISTS track_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, manga_id INTEGER NOT NULL, chapters TEXT NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
		db.execSQL("CREATE INDEX IF NOT EXISTS index_track_logs_manga_id ON track_logs (manga_id)")
	}
}
