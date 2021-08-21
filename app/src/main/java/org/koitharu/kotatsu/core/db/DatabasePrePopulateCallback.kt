package org.koitharu.kotatsu.core.db

import android.content.res.Resources
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.SortOrder

class DatabasePrePopulateCallback(private val resources: Resources) : RoomDatabase.Callback() {

	override fun onCreate(db: SupportSQLiteDatabase) {
		db.execSQL(
			"INSERT INTO favourite_categories (created_at, sort_key, title, `order`) VALUES (?,?,?,?)",
			arrayOf(System.currentTimeMillis(), 1, resources.getString(R.string.read_later), SortOrder.NEWEST.name)
		)
	}
}