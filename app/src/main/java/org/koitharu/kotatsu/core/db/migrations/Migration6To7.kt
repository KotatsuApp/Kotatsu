package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration6To7 : Migration(6, 7) {

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("ALTER TABLE manga ADD COLUMN public_url TEXT NOT NULL DEFAULT ''")
	}
}