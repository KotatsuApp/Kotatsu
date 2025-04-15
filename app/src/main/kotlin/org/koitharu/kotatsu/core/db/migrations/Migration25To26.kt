package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration25To26 : Migration(25, 26) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE sources ADD COLUMN cf_state INTEGER NOT NULL DEFAULT 0")
		db.execSQL("ALTER TABLE preferences ADD COLUMN title_override TEXT DEFAULT NULL")
		db.execSQL("ALTER TABLE preferences ADD COLUMN cover_override TEXT DEFAULT NULL")
		db.execSQL("ALTER TABLE preferences ADD COLUMN content_rating_override TEXT DEFAULT NULL")
		db.execSQL("ALTER TABLE favourites ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
		db.execSQL("ALTER TABLE tags ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
	}
}
