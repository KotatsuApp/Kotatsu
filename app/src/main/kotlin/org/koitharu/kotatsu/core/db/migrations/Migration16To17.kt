package org.koitharu.kotatsu.core.db.migrations

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.parsers.model.MangaSource

class Migration16To17(context: Context) : Migration(16, 17) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

	override fun migrate(database: SupportSQLiteDatabase) {
		database.execSQL("CREATE TABLE `sources` (`source` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `sort_key` INTEGER NOT NULL, PRIMARY KEY(`source`))")
		database.execSQL("CREATE INDEX `index_sources_sort_key` ON `sources` (`sort_key`)")
		val hiddenSources = prefs.getStringSet("sources_hidden", null).orEmpty()
		val order = prefs.getString("sources_order_2", null)?.split('|').orEmpty()
		val sources = MangaSource.entries
		for (source in sources) {
			if (source == MangaSource.LOCAL) {
				continue
			}
			val name = source.name
			var isEnabled = name !in hiddenSources
			var sortKey = order.indexOf(name)
			if (sortKey == -1) {
				sortKey = order.size + source.ordinal
				isEnabled = false
			}
			database.execSQL(
				"INSERT INTO `sources` (`source`, `enabled`, `sort_key`) VALUES (?, ?, ?)",
				arrayOf(name, isEnabled.toInt(), sortKey),
			)
		}
	}

	private fun Boolean.toInt() = if (this) 1 else 0
}
