package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration3To4 : Migration(3, 4) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("CREATE TABLE IF NOT EXISTS tracks (manga_id INTEGER NOT NULL, chapters_total INTEGER NOT NULL, last_chapter_id INTEGER NOT NULL, chapters_new INTEGER NOT NULL, last_check INTEGER NOT NULL, last_notified_id INTEGER NOT NULL, PRIMARY KEY(manga_id), FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE )")
	}
}
