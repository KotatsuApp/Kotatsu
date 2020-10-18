package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration1To2 : Migration(1, 2) {
	/**
	 * Adding foreign keys
	 */
	override fun migrate(database: SupportSQLiteDatabase) {
		/* manga_tags */
		database.execSQL(
			"CREATE TABLE IF NOT EXISTS manga_tags_tmp (manga_id INTEGER NOT NULL, tag_id INTEGER NOT NULL, " +
					"PRIMARY KEY(manga_id, tag_id), " +
					"FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
					"FOREIGN KEY(tag_id) REFERENCES tags(tag_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
		)
		database.execSQL("CREATE INDEX IF NOT EXISTS index_manga_tags_manga_id ON manga_tags_tmp (manga_id)")
		database.execSQL("CREATE INDEX IF NOT EXISTS index_manga_tags_tag_id ON manga_tags_tmp (tag_id)")
		database.execSQL("INSERT INTO manga_tags_tmp (manga_id, tag_id) SELECT manga_id, tag_id FROM manga_tags")
		database.execSQL("DROP TABLE manga_tags")
		database.execSQL("ALTER TABLE manga_tags_tmp RENAME TO manga_tags")
		/* favourites */
		database.execSQL(
			"CREATE TABLE IF NOT EXISTS favourites_tmp (manga_id INTEGER NOT NULL, category_id INTEGER NOT NULL, created_at INTEGER NOT NULL, " +
					"PRIMARY KEY(manga_id, category_id), " +
					"FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE , " +
					"FOREIGN KEY(category_id) REFERENCES favourite_categories(category_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
		)
		database.execSQL("CREATE INDEX IF NOT EXISTS index_favourites_manga_id ON favourites_tmp (manga_id)")
		database.execSQL("CREATE INDEX IF NOT EXISTS index_favourites_category_id ON favourites_tmp (category_id)")
		database.execSQL("INSERT INTO favourites_tmp (manga_id, category_id, created_at) SELECT manga_id, category_id, created_at FROM favourites")
		database.execSQL("DROP TABLE favourites")
		database.execSQL("ALTER TABLE favourites_tmp RENAME TO favourites")
		/* history */
		database.execSQL(
			"CREATE TABLE IF NOT EXISTS history_tmp (manga_id INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, chapter_id INTEGER NOT NULL, page INTEGER NOT NULL, " +
					"PRIMARY KEY(manga_id), " +
					"FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
		)
		database.execSQL("INSERT INTO history_tmp (manga_id, created_at, updated_at, chapter_id, page) SELECT manga_id, created_at, updated_at, chapter_id, page FROM history")
		database.execSQL("DROP TABLE history")
		database.execSQL("ALTER TABLE history_tmp RENAME TO history")
		/* preferences */
		database.execSQL(
			"CREATE TABLE IF NOT EXISTS preferences_tmp (manga_id INTEGER NOT NULL, mode INTEGER NOT NULL," +
					" PRIMARY KEY(manga_id), " +
					"FOREIGN KEY(manga_id) REFERENCES manga(manga_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
		)
		database.execSQL("INSERT INTO preferences_tmp (manga_id, mode) SELECT manga_id, mode FROM preferences")
		database.execSQL("DROP TABLE preferences")
		database.execSQL("ALTER TABLE preferences_tmp RENAME TO preferences")
	}
}