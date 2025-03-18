package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration24To25 : Migration(24, 25) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE manga ADD COLUMN content_rating TEXT DEFAULT NULL")
		db.execSQL("UPDATE manga SET content_rating = 'ADULT' WHERE nsfw = 1")
	}
}
