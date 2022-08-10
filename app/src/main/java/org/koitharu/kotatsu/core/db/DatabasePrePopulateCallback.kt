package org.koitharu.kotatsu.core.db

import android.content.res.Resources
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.model.SortOrder

class DatabasePrePopulateCallback(private val resources: Resources) : RoomDatabase.Callback() {

	override fun onCreate(db: SupportSQLiteDatabase) {
		db.execSQL(
			"INSERT INTO favourite_categories (created_at, sort_key, title, `order`, track, show_in_lib, `deleted_at`) VALUES (?,?,?,?,?,?,?)",
			arrayOf(
				System.currentTimeMillis(),
				1,
				resources.getString(R.string.read_later),
				SortOrder.NEWEST.name,
				1,
				1,
				0L,
			)
		)
	}
}
