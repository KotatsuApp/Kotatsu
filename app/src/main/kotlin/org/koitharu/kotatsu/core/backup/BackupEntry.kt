package org.koitharu.kotatsu.core.backup

import org.json.JSONArray

class BackupEntry(
	val name: Name,
	val data: JSONArray
) {

	enum class Name(
		val key: String,
	) {

		INDEX("index"),
		HISTORY("history"),
		CATEGORIES("categories"),
		FAVOURITES("favourites"),
		SETTINGS("settings"),
		SETTINGS_READER_GRID("reader_grid"),
		BOOKMARKS("bookmarks"),
		SOURCES("sources"),
	}
}
