package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration11To12 : Migration(11, 12) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL(
			"""
			CREATE TABLE IF NOT EXISTS `scrobblings` (
				`scrobbler` INTEGER NOT NULL,
				`id` INTEGER NOT NULL,
				`manga_id` INTEGER NOT NULL,
				`target_id` INTEGER NOT NULL, 
				`status` TEXT,
				`chapter` INTEGER NOT NULL, 
				`comment` TEXT,
				`rating` REAL NOT NULL,
				PRIMARY KEY(`scrobbler`, `id`, `manga_id`)
			)
			""".trimIndent()
		)
	}
}