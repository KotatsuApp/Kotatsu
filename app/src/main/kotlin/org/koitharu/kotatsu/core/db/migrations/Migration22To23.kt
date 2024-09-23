package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration22To23 : Migration(22, 23) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("CREATE TABLE IF NOT EXISTS `local_index` (`manga_id` INTEGER NOT NULL, `path` TEXT NOT NULL, PRIMARY KEY(`manga_id`), FOREIGN KEY(`manga_id`) REFERENCES `manga`(`manga_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
	}
}
